package net.corda.training.contract

import net.corda.core.contracts.*
import net.corda.core.crypto.keys
import net.corda.core.identity.Party
import net.corda.core.messaging.RPCReturnsObservables
import net.corda.core.transactions.LedgerTransaction
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.utils.sumCash
import net.corda.training.state.IOUState
import java.security.PublicKey

/**
 * This is where you'll add the contract code which defines how the [IOUState] behaves. Look at the unit tests in
 * [IOUContractTests] for instructions on how to complete the [IOUContract] class.
 */
class IOUContract : Contract {
    companion object {
        @JvmStatic
        val IOU_CONTRACT_ID = "net.corda.training.contract.IOUContract"
    }

    /**
     * Add any commands required for this contract as classes within this interface.
     * It is useful to encapsulate your commands inside an interface, so you can use the [requireSingleCommand]
     * function to check for a number of commands which implement this interface.
     */
    interface Commands : CommandData {
        class Issue: TypeOnlyCommandData(), Commands
        class Settle: TypeOnlyCommandData(), Commands
        class Transfer: TypeOnlyCommandData(), Commands
    }

    /**
     * The contract code for the [IOUContract].
     * The constraints are self documenting so don't require any additional explanation.
     */
    override fun verify(tx: LedgerTransaction) {

        val command = tx.commands.requireSingleCommand<IOUContract.Commands>()

        when (command.value) {

            is Commands.Issue -> requireThat {
                "No inputs should be consumed when issuing an IOU." using ( tx.inputStates.size == 0 )
                "Only one output state should be created when issuing an IOU." using (tx.outputStates.size == 1)

                val outputState = tx.outputStates.single() as IOUState
                "A newly issued IOU must have a positive amount." using (outputState.amount.quantity > 0)
                "The lender and borrower cannot have the same identity." using (outputState.lender != outputState.borrower)

                val signingParties = tx.commands.requireSingleCommand<Commands.Issue>().signers.toSet()
                val participants = tx.outputStates.single().participants.map{ it -> it.owningKey }
                "Both lender and borrower together only may sign IOU issue transaction." using(signingParties.containsAll<PublicKey>(participants) && signingParties.size == 2)
            }

            is Commands.Transfer -> requireThat {
                "An IOU transfer transaction should only consume one input state." using (tx.inputStates.size == 1)
                "An IOU transfer transaction should only create one output state." using (tx.outputStates.size == 1)

                // Copy of input with new lender
                val checkOutputState = tx.outputStates.single() as IOUState
                val checkInputState = tx.inputStates.single() as IOUState
                "Only the lender property may change." using (checkOutputState.withNewLender(checkInputState.lender) == checkInputState)
                "The lender property must change in a transfer." using (checkOutputState.lender != checkInputState.lender)
                val listOfPublicKeys = listOf(checkInputState.lender.owningKey, checkInputState.borrower.owningKey, checkOutputState.lender.owningKey)
                System.out.println("HEYLOOKFORMEWHYDON'TYA")
                System.out.println(command.signers)
                System.out.println(command.signers.size)
                "The borrower, old lender and new lender only must sign an IOU transfer transaction" using (command.signers.containsAll(listOfPublicKeys) && command.signers.size == 3)
            }

            is Commands.Settle -> requireThat {
                val groupStates = tx.groupStates<IOUState, UniqueIdentifier> { it.linearId }
                "There must be one input IOU." using (tx.inputStates.size > 0)
                "List has more than one element." using (groupStates.size < 2)
                "There must be output cash." using ( tx.outputsOfType<Cash.State>().size > 0 )

                //sum the cash states
                val cashStates = tx.outputsOfType<Cash.State>()
                val sum = cashStates.sumCash().withoutIssuer()
                val inputAmount = tx.inputsOfType<IOUState>().single()
                "The amount settled cannot be more than the amount outstanding." using (inputAmount.amount >= sum)

                val checkOutput = tx.inputsOfType<IOUState>().single()
                val ourCash = cashStates.filter { it.owner.owningKey == checkOutput.lender.owningKey }

                "There must be output cash paid to the recipient." using ( ourCash.size > 0 )

                val ourTotalCashAmount = ourCash.sumCash().quantity


                val inputState = tx.inputsOfType<IOUState>().single()

                if (inputState.amount.quantity > ourTotalCashAmount) {

                    val outputStates = tx.outputsOfType<IOUState>()
                    "There must be one output IOU." using ( outputStates.size == 1 )

                    "The borrower may not change when settling." using ( inputState.borrower == outputStates.single().borrower )
                    "The amount may not change when settling." using ( inputState.amount == outputStates.single().amount )
                    "The lender may not change when settling." using ( inputState.lender == outputStates.single().lender )

                } else {

                    "There must be no output IOU as it has been fully settled." using ( tx.outputsOfType<IOUState>().size == 0 )

                }

                "Both lender and borrower together only must sign IOU settle transaction." using (
                        command.signers.toSet() == inputState.participants.map{ it -> it.owningKey }.toSet()
                        )

            }

        }



    }
}
