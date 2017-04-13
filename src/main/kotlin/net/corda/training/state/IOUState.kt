package net.corda.training.state

import net.corda.core.contracts.ContractState
import net.corda.core.crypto.CompositeKey
import net.corda.training.contract.IOUContract

data class IOUState(val data: String = "data"): ContractState {
    override val participants: List<CompositeKey> get() = listOf()

    /**
     * A Contract code reference to the IOUCoontract. Make sure this is not part of the [IOUState] constructor.
     */
    override val contract get() = IOUContract()
}