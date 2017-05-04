package net.corda.training.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.contracts.asset.Cash
import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowLogic
import net.corda.core.serialization.OpaqueBytes
import net.corda.flows.CashIssueFlow
import java.util.*

/**
 * This is a flow which has been implemented for you to self issue cash for a node on the ledger.
 */
class SelfIssueCashFlow(val amount: Amount<Currency>) : FlowLogic<Cash.State>() {
    @Suspendable
    override fun call(): Cash.State {
        /** Create the cash issue command. */
        val issueRef = OpaqueBytes.of(0)
        val notary = serviceHub.networkMapCache.notaryNodes.single().notaryIdentity
        val me = serviceHub.myInfo.legalIdentity
        /** Create the cash issuance transaction. */
        val cashIssueTransaction = subFlow(CashIssueFlow(amount, issueRef, me, notary))
        /** Return the cash output. */
        return cashIssueTransaction.tx.outputs.single().data as Cash.State
    }
}
