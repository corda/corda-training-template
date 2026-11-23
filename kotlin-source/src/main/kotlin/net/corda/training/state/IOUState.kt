package net.corda.training.state

import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.Party
import net.corda.training.contract.IOUContract
import java.util.Currency

@BelongsToContract(IOUContract::class)
data class IOUState(
    val amount: Amount<Currency>
) : ContractState {
    override val participants: List<Party> get() = listOf()
}
