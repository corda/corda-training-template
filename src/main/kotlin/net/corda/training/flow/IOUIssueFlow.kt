package net.corda.training.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.TransactionType
import net.corda.core.crypto.Party
import net.corda.core.flows.FlowLogic
import net.corda.core.transactions.SignedTransaction
import net.corda.flows.FinalityFlow
import net.corda.training.contract.IOUContract
import net.corda.training.state.IOUState

/**
 * This is the flow which handles issuance of new IOUs on the ledger.
 * This flow doesn't come in an Initiator and Responder pair as messaging across the network is handled by a [subFlow]
 * call to [CollectSignatureFlow.Initiator].
 * Notarisation (if required) and commitment to the ledger is handled vy the [FinalityFlow].
 * The flow returns the [SignedTransaction] that was committed to the ledger.
 */
class IOUIssueFlow(val state: IOUState, val otherParty: Party): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        // Step 1. Get a reference to the notary service on our network and our key pair.
        val notary = serviceHub.networkMapCache.notaryNodes.single().notaryIdentity
        // Step 2. Create a new issue command.
        // Remember that a command is a CommandData object and a list of CompositeKeys
        val issueCommand = Command(IOUContract.Commands.Issue(), state.participants)
        // Step 3. Create a new TransactionBuilder object.
        val builder = TransactionType.General.Builder(notary)
        // Step 4. Add the iou as an output state, as well as a command to the transaction builder.
        builder.withItems(state, issueCommand)
        // Step 5. Verify and aign it with our KeyPair.
        builder.toWireTransaction().toLedgerTransaction(serviceHub).verify()
        val ptx = builder.signWith(serviceHub.legalIdentityKey).toSignedTransaction(false)
        // Step 6. Collect the other party's signature using the SignTransactionFlow.
        val stx = subFlow(SignTransactionFlow.Initiator(ptx))
        // Step 7. Assuming no exceptions, we can now finalise the transaction.
        val ftx = subFlow(FinalityFlow(stx, setOf(serviceHub.myInfo.legalIdentity, otherParty))).single()
        return ftx
    }
}