package net.corda.training.state

import net.corda.core.contracts.Amount
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.Party
import net.corda.core.crypto.CompositeKey
import net.corda.core.crypto.NullPublicKey
import net.corda.core.crypto.keys
import net.corda.training.contract.IOUContract
import java.security.PublicKey
import java.util.*

/**
 * The IOU State object, with the following properties:
 * - [amount] The amount owed from the [borrower] to the [lender]
 * - [lender] The lending party.
 * - [borrower] The borrowing party.
 * - [contract] Holds a reference to the [IOUContract]
 * - [paid] Records how much of the [amount] has been paid.
 * - [linearId] A unique id shared by all LinearState states throughout history within the vaults of all parties.
 *   Verify methods should check that one input and one output share the id in a transaction, except at
 *   issuance/termination.
 */
data class IOUState(val amount: Amount<Currency>,
               val lender: Party,
               val borrower: Party,
               override val contract: IOUContract,
               val paid: Amount<Currency> = Amount(0, amount.token),
               override val linearId: UniqueIdentifier = UniqueIdentifier()): LinearState {
    /**
     * This function determines if the [IOUState] is relevant to a Corda node based on whether the public keys
     * of the lender or borrower are known to the node, i.e. if the node is the lender or borrower.
     *
     * We do this by checking that the set intersection of the vault public keys with the participant public keys
     * is not the empty set.
     */
    override fun isRelevant(ourKeys: Set<PublicKey>): Boolean = ourKeys.intersect(participants.keys).isNotEmpty()

    /**
     *  This property holds a list of the public keys which belong to the nodes which can "use" this state in a valid
     *  transaction. In this case, the lender or the borrower.
     */
    override val participants: List<CompositeKey> get() = listOf(lender.owningKey, borrower.owningKey)

    /**
     * A toString() helper method for displaying IOUs in the console.
     */
    override fun toString() = "${borrower.name} owes ${lender.name} $amount and has paid $paid so far."

    /**
     * A helper methods for when building transactions for settling and transferring IOUs.
     * - [pay] adds an amount to the paid property. It does no validation.
     * - [withNewLender] creates a copy of the current state with a newly specified lender. For use when transferring.
     * - [withoutLender] is useful for checking that all properties apart from the lender remain unchanged between
     *   input and output states.
     */
    fun pay(amountToPay: Amount<Currency>) = copy(paid = paid.plus(amountToPay))
    fun withNewLender(newLender: Party) = copy(lender = newLender)
    fun withoutLender() = copy(lender = Party("", NullPublicKey))
}