package net.corda.training.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.training.contract.IOUContract
import net.corda.training.state.IOUState

/**
 * This is the flow which handles issuance of new IOUs on the ledger.
 * Gathering the counterparty signatures is handled by the [CollectSignaturesFlow].
 * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
 * The flow returns the [SignedTransaction] that was committed to the ledger.
 */
@InitiatingFlow
@StartableByRPC
class IOUIssueFlow(val state: IOUState) : FlowLogic<SignedTransaction>() {

    override val progressTracker = tracker()

    companion object {
        object CREATING : ProgressTracker.Step("Creating a new IOU")
        object VERIFYING : ProgressTracker.Step("Verifying the IOU")
        object SIGNING : ProgressTracker.Step("Signing the IOU")
        object STORING : ProgressTracker.Step("Processing storage of IOU") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(CREATING, VERIFYING, SIGNING, STORING)
    }

    @Suspendable
    override fun call(): SignedTransaction {
        progressTracker.currentStep = CREATING
        val notary = serviceHub.networkMapCache.notaryIdentities.single()
//        Normally we should specify the notary explicitly but for this example we know there is only one
//        val notaryName: CordaX500Name = CordaX500Name(
//            organisation = "Notary Service",
//            locality = "London",
//            country = "GB")
//        val specificNotary: Party = serviceHub.networkMapCache.getNotary(notaryName)!!

        val participants = state.participants
        val command = Command(IOUContract.Commands.Issue(), participants.map { it.owningKey })
        val stateAndContract = StateAndContract(state, IOUContract.IOU_CONTRACT_ID)
        val utx = TransactionBuilder(notary = notary).withItems(stateAndContract, command)

        progressTracker.currentStep = VERIFYING
        utx.verify(serviceHub)

        progressTracker.currentStep = SIGNING
        val stx = serviceHub.signInitialTransaction(utx)
        val counterpartySessions = participants.filterNot { it == ourIdentity }.map { initiateFlow(it) }
        val ctx = subFlow(CollectSignaturesFlow(stx, counterpartySessions))

        return subFlow(FinalityFlow(ctx, counterpartySessions))
    }
}

/**
 * This is the flow which signs IOU issuances.
 * The signing is handled by the [SignTransactionFlow].
 */
@InitiatedBy(IOUIssueFlow::class)
class IOUIssueFlowResponder(val flowSession: FlowSession): FlowLogic<Unit>() {
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