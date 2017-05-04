package net.corda.training.state

import net.corda.core.contracts.Amount
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.CompositeKey
import net.corda.core.crypto.Party
import net.corda.core.crypto.keys
import net.corda.training.contract.IOUContract
import java.security.PublicKey
import java.util.*

/**
 * This is where you'll add the definition of your state object. Look at the unit tests in [IOUStateTests] for
 * instructions on how to complete the [IOUState] class.
 *
 * Remove the "val data: String = "data" property before starting the [IOUState] tasks.
 */
data class IOUState(val data: String = "data"): ContractState {
    override val participants: List<CompositeKey> get() = listOf()

    /**
     * A Contract code reference to the IOUContract. Make sure this is not part of the [IOUState] constructor.
     * **Don't change this definition!**
     */
    override val contract get() = IOUContract()
}