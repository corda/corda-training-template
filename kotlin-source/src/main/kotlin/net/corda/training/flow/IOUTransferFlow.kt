package net.corda.training.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.*
import net.corda.core.crypto.Party
import net.corda.core.flows.FlowLogic
import net.corda.core.node.services.linearHeadsOfType
import net.corda.core.transactions.SignedTransaction
import net.corda.flows.FinalityFlow
import net.corda.training.contract.IOUContract
import net.corda.training.state.IOUState

/**
 * This is the flow which handles transfers of existing IOUs on the ledger.
 * This flow doesn't come in an Initiator and Responder pair as messaging across the network is handled by a [subFlow]
 * call to [CollectSignatureFlow.Initiator].
 * Notarisation (if required) and commitment to the ledger is handled vy the [FinalityFlow].
 * The flow returns the [SignedTransaction] that was committed to the ledger.
 */
class IOUTransferFlow(val linearId: UniqueIdentifier, val newLender: Party): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val me = serviceHub.myInfo.legalIdentity
        // Stage 1. Retrieve IOU specified by linearId from the vault.
        val iouStates = serviceHub.vaultService.linearHeadsOfType<IOUState>()
        val iouStateAndRef = iouStates[linearId]!!
        val inputIou = iouStateAndRef.state.data
        // Stage 2. This flow can only be initiated by the current recipient.
        if (serviceHub.myInfo.legalIdentity != inputIou.lender) {
            throw IllegalArgumentException("IOU transfer can only be initiated by the IOU lender.")
        }
        // Stage 3. Create the new IOU state reflecting a new lender.
        val outputIou = inputIou.withNewLender(newLender)
        // Stage 4. Create the transfer command.
        val signers = inputIou.participants.plus(newLender.owningKey)
        val transferCommand = Command(IOUContract.Commands.Transfer(), signers)
        // Stage 5. Get a reference to a transaction builder.
        val notary = serviceHub.networkMapCache.notaryNodes.single().notaryIdentity
        val builder = TransactionType.General.Builder(notary)
        // Stage 6. Create the transaction which comprises: one input, one output and one command.
        builder.withItems(iouStateAndRef, outputIou, transferCommand)
        // Stage 7. Verify and sign.
        builder.toWireTransaction().toLedgerTransaction(serviceHub).verify()
        val keyPair = serviceHub.legalIdentityKey
        val ptx = builder.signWith(keyPair).toSignedTransaction(false)
        // Stage 8. Collect signature from borrower and add it to the transaction.
        val borrowerSig = subFlow(CollectSignatureFlow.Initiator(
                ptx,
                mutableSetOf(me.owningKey),
                inputIou.borrower))
        val ptxTwo = ptx + borrowerSig
        // Stage 9. Collected signatures from new lender and add it to the transaction.
        val newLenderSig = subFlow(CollectSignatureFlow.Initiator(
                ptxTwo,
                mutableSetOf(me.owningKey, inputIou.borrower.owningKey),
                newLender))
        val stx = ptxTwo + newLenderSig
        // Stage 9. Notarise and record,
        subFlow(FinalityFlow(stx, setOf(inputIou.lender, inputIou.borrower, newLender)))
        return stx
    }
}
