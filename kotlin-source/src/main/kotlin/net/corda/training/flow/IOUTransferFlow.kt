package net.corda.training.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.*
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.training.contract.IOUContract
import net.corda.training.state.IOUState
import java.lang.IllegalArgumentException

/**
 * This is the flow which handles transfers of existing IOUs on the ledger.
 * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
 * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
 * The flow returns the [SignedTransaction] that was committed to the ledger.
 */
@InitiatingFlow
@StartableByRPC
class IOUTransferFlow(val linearId: UniqueIdentifier, val newLender: Party): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        // Get components from the flow
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        val iouStateAndRef =  serviceHub.vaultService.queryBy(IOUState::class.java, queryCriteria).states.single()
        val inputState = iouStateAndRef.state.data
        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        // Add command to the flow
        val tb = TransactionBuilder(notary)
        val command = Command(IOUContract.Commands.Transfer(), listOf(inputState.borrower.owningKey, inputState.lender.owningKey, newLender.owningKey))
        tb.addCommand(command)

        // Add States to the flow
        val convertedInputState = TransactionState(inputState, IOUContract.IOU_CONTRACT_ID , notary)
        tb.withItems(iouStateAndRef, StateAndContract(convertedInputState.data.withNewLender(newLender), IOUContract.IOU_CONTRACT_ID))

        // Check lender is running the flow
        if (inputState.lender != ourIdentity) {
            throw IllegalArgumentException("This flow must be run by the current lender.")
        }

        // Verify the transaction
        tb.verify(serviceHub)

        // Sign the transaction
        val partiallySignedTransaction = serviceHub.signInitialTransaction(tb)

        // Collect Signatures
        val listOfFlows = (inputState.participants - ourIdentity + newLender).map{ it -> initiateFlow(it) }.toSet()
        val fullySignedTransaction = subFlow(CollectSignaturesFlow(partiallySignedTransaction, listOfFlows))

        return subFlow(FinalityFlow(fullySignedTransaction))

    }
}

/**
 * This is the flow which signs IOU transfers.
 * The signing is handled by the [SignTransactionFlow].
 */
@InitiatedBy(IOUTransferFlow::class)
class IOUTransferFlowResponder(val flowSession: FlowSession): FlowLogic<Unit>() {
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