package net.corda.training.flow

import net.corda.core.crypto.Party
import net.corda.core.transactions.WireTransaction

/**
 * This is a Dummy implementation of the [SignTransactionFlow.AbstractResponder].
 * In practise, developers should implement some additional logic within the [checkTransaction] function that
 * ascertains whether the proposed transaction is one the node should sign.
 */
class SignTransactionFlowResponder(override val otherParty: Party): SignTransactionFlow.AbstractResponder() {
    override fun checkTransaction(wtx: WireTransaction) {
        // Do some additional checking here if required.
    }
}
