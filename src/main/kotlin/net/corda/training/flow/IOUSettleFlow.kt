package net.corda.training.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.contracts.asset.Cash
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.OpaqueBytes
import net.corda.flows.CashIssueFlow
import net.corda.training.contract.IOUContract
import net.corda.training.state.IOUState
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
        // Placeholder code to avoid type error when running the tests. Remove before starting the flow task!
        return serviceHub.signInitialTransaction(
                TransactionBuilder(notary = null)
        )
    }
}

/**
 * This is the flow which signs IOU settlements.
 * The signing is handled by the [CollectSignaturesFlow].
 */
@InitiatedBy(IOUSettleFlow::class)
class IOUSettleFlowResponder(val otherParty: Party): FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val signTransactionFlow = object : SignTransactionFlow(otherParty) {
            override fun checkTransaction(stx: SignedTransaction) {
                // Define checking logic.
            }
        }

        subFlow(signTransactionFlow)
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
        val notary = serviceHub.networkMapCache.getAnyNotary()!!
        val me = serviceHub.myInfo.legalIdentity
        /** Create the cash issuance transaction. */
        val cashIssueTransaction = subFlow(CashIssueFlow(amount, issueRef, me, notary, false))
        /** Return the cash output. */
        return cashIssueTransaction.stx.tx.outputs.single().data as Cash.State
    }
}