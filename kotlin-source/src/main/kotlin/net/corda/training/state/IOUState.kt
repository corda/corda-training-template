package net.corda.training.state

import net.corda.core.contracts.Amount
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.AnonymousParty
import net.corda.core.crypto.CompositeKey
import net.corda.core.crypto.keys
import net.corda.training.contract.TemplateContract
import java.security.PublicKey
import java.util.*

/**
 * The IOU State object.
 */
class IOUState(val amount: Amount<Currency>,
               val lender: AnonymousParty,
               val borrower: AnonymousParty,
               val paid: Amount<Currency>,
               override val contract: TemplateContract,
               override val linearId: UniqueIdentifier = UniqueIdentifier()): LinearState {
    /**
     * This function determins if the [IOUState] is relevant to a Corda node based on whether the public keys
     * of the lender or borrower are known to the node, i.e. if the node is the lender or borrower.
     */
    override fun isRelevant(ourKeys: Set<PublicKey>): Boolean = ourKeys.intersect(participants.keys).isNotEmpty()
    /** The public keys of the involved parties. */
    override val participants: List<CompositeKey>
        get() = listOf(lender.owningKey, borrower.owningKey)
}