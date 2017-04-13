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
 * This is the flow which handles transferring existing IOUs on the ledger to another [Party].
 * Look at the unit tests in [IOUTransferFlowTests] for how to complete the [call] method of this class.
 */
class IOUTransferFlow(val linearId: UniqueIdentifier, val newLender: Party): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        // Placeholder code to avoid typing error.
        // Placeholder code to avoid typing error. Remove before starting the flow task!
        return TransactionType.General.Builder(null).toSignedTransaction(false)
    }
}
