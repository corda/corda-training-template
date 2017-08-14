package net.corda.training.state

import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.training.contract.IOUContract

/**
 * This is where you'll add the definition of your state object. Look at the unit tests in [IOUStateTests] for
 * instructions on how to complete the [IOUState] class.
 *
 * Remove the "val data: String = "data" property before starting the [IOUState] tasks.
 */
data class IOUState(val data: String = "data"): ContractState {
    override val participants: List<AbstractParty> get() = listOf()

    /**
     * A Contract code reference to the IOUContract. Make sure this is not part of the [IOUState] constructor.
     * **Don't change this definition!**
     */
    override val contract get() = IOUContract()
}