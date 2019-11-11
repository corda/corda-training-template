package net.corda.training.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.training.contract.IOUContract
import net.corda.training.state.IOUState

/**
 * This is the flow which handles transfers of existing IOUs on the ledger.
 * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
 * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
 * The flow returns the [SignedTransaction] that was committed to the ledger.
 */
@InitiatingFlow
@StartableByRPC
class IOUTransferFlow(val linearId: UniqueIdentifier, val newLender: Party) : FlowLogic<SignedTransaction>() {

    override val progressTracker = tracker()

    companion object {
        object CREATING : ProgressTracker.Step("Creating a transfer IOU transaction")
        object VERIFYING : ProgressTracker.Step("Verifying the IOU transfer")
        object SIGNING : ProgressTracker.Step("Signing the IOU transfer")
        object STORING : ProgressTracker.Step("Processing storage of IOU transfer") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(CREATING, SIGNING, VERIFYING, STORING)
    }

    @Suspendable
    override fun call(): SignedTransaction {
        progressTracker.currentStep = CREATING
        val notary = serviceHub.networkMapCache.notaryIdentities.single()

        val inputStateAndRef = getInputStateAndRef(linearId)
        val (utx, participants) = createUnsignedTransaction(notary, inputStateAndRef)

        progressTracker.currentStep = VERIFYING
        utx.verify(serviceHub)

        progressTracker.currentStep = SIGNING
        val stx = serviceHub.signInitialTransaction(utx)

        val counterpartySessions = participants.filterNot { it == ourIdentity }.map { initiateFlow(it) }
        val ctx = subFlow(CollectSignaturesFlow(stx, counterpartySessions))

        return subFlow(FinalityFlow(ctx, counterpartySessions))
    }

    private fun getInputStateAndRef(linearId: UniqueIdentifier): StateAndRef<IOUState> {
        val linearStateCriteria =
            QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<IOUState>(linearStateCriteria).states.single()
    }

    private fun createUnsignedTransaction(
        notary: Party,
        inputStateAndRef: StateAndRef<IOUState>
    ): Pair<TransactionBuilder, List<Party>> {
        val inputIOU = inputStateAndRef.state.data
        val participants = inputStateAndRef.state.data.participants + newLender

        if (inputIOU.lender != ourIdentity) {
            throw IllegalArgumentException("Only the current lender can initiate an IOU transfer")
        }

        val outputIOU = inputIOU.withNewLender(newLender)
        val command = Command(IOUContract.Commands.Transfer(), participants.map { it.owningKey })
        val outputStateAndContract = StateAndContract(outputIOU, IOUContract.IOU_CONTRACT_ID)
        val transactionBuilder = TransactionBuilder(notary = notary).withItems(
            inputStateAndRef, outputStateAndContract, command
        )

        return Pair(transactionBuilder, participants)
    }
}

/**
 * This is the flow which signs IOU transfers.
 * The signing is handled by the [SignTransactionFlow].
 */
@InitiatedBy(IOUTransferFlow::class)
class IOUTransferFlowResponder(val flowSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an IOU transaction" using (output is IOUState)
            }
        }

        val signedTxID = subFlow(signedTransactionFlow).id
        subFlow(ReceiveFinalityFlow(flowSession, expectedTxId = signedTxID))
    }
}