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
import java.util.*

/**
 * This is the flow which handles settlement of existing IOUs on the ledger.
 * Look at the unit tests in [IOUSettleFlowTests] for how to complete the [call] method of this class.
 */
class IOUSettleFlow(val linearId: UniqueIdentifier, val amount: Amount<Currency>): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        // Placeholder code to avoid type error when running the tests. Remove before starting the flow task!
        return TransactionType.General.Builder(null).toSignedTransaction(false)
    }
}
