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
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.OpaqueBytes
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.contracts.asset.PartyAndAmount
import net.corda.finance.contracts.getCashBalance
import net.corda.finance.flows.CashIssueFlow
import net.corda.training.contract.IOUContract
import net.corda.training.state.IOUState
import java.lang.IllegalArgumentException
import java.util.*

/**
 * This is the flow which handles the (partial) settlement of existing IOUs on the ledger.
 * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
 * Notarisation (if required) and commitment to the ledger is handled vy the [FinalityFlow].
 * The flow returns the [SignedTransaction] that was committed to the ledger.
 */
@InitiatingFlow
@StartableByRPC
class IOUSettleFlow(val linearId: UniqueIdentifier, val amount: Amount<Currency>): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        // Get all components for building transaction
        val customQueryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        val stateAndRefToSettle = serviceHub.vaultService.queryBy(IOUState::class.java, customQueryCriteria ).states.single()
        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        // Throw exception if borrower is not running the flow
        if (stateAndRefToSettle.state.data.borrower != ourIdentity) {
            throw IllegalArgumentException("The borrower must issue the flow")
        }

        // Throw exception if borrower has no cash
        if (serviceHub.getCashBalance(stateAndRefToSettle.state.data.amount.token).quantity <= 0) {
            throw IllegalArgumentException("The borrower must have some cash")
        }

        // Throw exception if borrower has no cash
        if (serviceHub.getCashBalance(stateAndRefToSettle.state.data.amount.token).quantity < amount.quantity) {
            throw IllegalArgumentException("The borrower must have enough cash")
        }

        // Create a settled output state
        val settledState = stateAndRefToSettle.state.data.pay(amount)

        // Add a command to the flow
        val tb = TransactionBuilder(notary)
        val command = Command(IOUContract.Commands.Settle(), listOf(ourIdentity.owningKey, stateAndRefToSettle.state.data.lender.owningKey))
        tb.addCommand(command)

        // Add states to transaction
        val tx = tb.withItems(stateAndRefToSettle, StateAndContract(settledState, IOUContract.IOU_CONTRACT_ID))

        //generateSpend(services, tx, amount, to, ourIdentity, onlyFromParties
        Cash.generateSpend(serviceHub, tx, amount, ourIdentityAndCert, settledState.lender, setOf(ourIdentity))

        val ptx = serviceHub.signInitialTransaction(tx)

        val listOfFlows = (stateAndRefToSettle.state.data.participants - ourIdentity).map{ it -> initiateFlow(it) }
        val stx = subFlow(CollectSignaturesFlow(ptx, listOfFlows))

        return subFlow(FinalityFlow(stx))
    }
}

/**
 * This is the flow which signs IOU settlements.
 * The signing is handled by the [SignTransactionFlow].
 */
@InitiatedBy(IOUSettleFlow::class)
class IOUSettleFlowResponder(val flowSession: FlowSession): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val outputStates = stx.tx.outputs.map { it.data::class.java.name }.toList()
                "There must be an IOU transaction." using (outputStates.contains(IOUState::class.java.name))
            }
        }

        subFlow(signedTransactionFlow)
    }
}

@InitiatingFlow
@StartableByRPC
/**
 * Self issues the calling node an amount of cash in the desired currency.
 * Only used for demo/sample/training purposes!
 */
class SelfIssueCashFlow(val amount: Amount<Currency>) : FlowLogic<Cash.State>() {
    @Suspendable
    override fun call(): Cash.State {
        /** Create the cash issue command. */
        val issueRef = OpaqueBytes.of(0)
        /** Note: ongoing work to support multiple notary identities is still in progress. */
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        /** Create the cash issuance transaction. */
        val cashIssueTransaction = subFlow(CashIssueFlow(amount, issueRef, notary))
        /** Return the cash output. */
        return cashIssueTransaction.stx.tx.outputs.single().data as Cash.State
    }
}