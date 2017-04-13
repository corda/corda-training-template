package net.corda.training.contract

import net.corda.contracts.asset.Cash
import net.corda.contracts.asset.sumCash
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.by
import net.corda.core.crypto.SecureHash
import net.corda.training.state.IOUState

/**
 * The IOUContract can handle a three transaction types involving [IOUState]s.
 * - Issuance: Issuing a new [IOUState] on the ledger, which is a bilateral agreement between two parties.
 * - Transfer: Re-assinging the lender/beneficiary.
 * - Settle: Fully or partially settling the [IOUState] using the Corda [Cash] contract.
 */
class IOUContract : Contract {
    /**
     * Legal prose reference. This is just a dummy string for the time being.
     */
    override val legalContractReference: SecureHash = SecureHash.sha256("Prose contract.")

    /**
     * Add any commands required for this contract as classes within this interface.
     * It is useful to encapsulate your commands inside an interface, so you can use the [requireSingleCommand]
     * function to check for a range of commands which implement this interface.
     */
    interface Commands : CommandData {

    }

    /**
     * The contract code for the [IOUContract].
     * The constraints are self documenting so don't require any additional explanation.
     */
    override fun verify(tx: TransactionForContract) {

    }
}
