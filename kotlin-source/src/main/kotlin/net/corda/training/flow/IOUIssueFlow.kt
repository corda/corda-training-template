package net.corda.training.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.requireThat
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.training.contract.IOUContract
import net.corda.training.state.IOUState

/**
 * This is the flow which handles issuance of new IOUs on the ledger.
 * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
 * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
 * The flow returns the [SignedTransaction] that was committed to the ledger.
 */
@InitiatingFlow
@StartableByRPC
class IOUIssueFlow(val state: IOUState) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        // Get component pieces of flow
        val notary = serviceHub.networkMapCache.notaryIdentities.single()
        val tb = TransactionBuilder(notary)
        val command = Command(IOUContract.Commands.Issue(), listOf(state.borrower.owningKey, state.lender.owningKey))

        // Add command to flow
        tb.addCommand(command)

        // Add input / output states to flow
        val convertedState = StateAndContract(state, IOUContract.IOU_CONTRACT_ID)
        tb.withItems(convertedState)

        // Verify the transaction
        tb.verify(serviceHub)

        // Sign the transaction
        val partiallySignedTransaction = serviceHub.signInitialTransaction(tb)

        // Collect Signatures

        val listOfFlows = (state.participants - ourIdentity).map{ it -> initiateFlow(it) }.toSet()

        System.out.println(listOfFlows)

        val fullySignedTransaction = subFlow(CollectSignaturesFlow(partiallySignedTransaction, listOfFlows))

        return subFlow(FinalityFlow(fullySignedTransaction))

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
        subFlow(signedTransactionFlow)
    }
}