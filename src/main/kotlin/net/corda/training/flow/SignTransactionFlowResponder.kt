package net.corda.training.flow

import net.corda.core.crypto.Party
import net.corda.core.transactions.WireTransaction

class SignTransactionFlowResponder(override val otherParty: Party): SignTransactionFlow.AbstractResponder() {
    override fun checkTransaction(wtx: WireTransaction) {
        // Do some additional checking here if required.
    }
}
