package net.corda.training.contract

import net.corda.core.contracts.*
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.contracts.utils.sumCash
import net.corda.training.state.IOUState

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
        class Issue : TypeOnlyCommandData(), Commands
        class Transfer : TypeOnlyCommandData(), Commands
        class Settle : TypeOnlyCommandData(), Commands
    }

    /**
     * The contract code for the [IOUContract].
     * The constraints are self documenting so don't require any additional explanation.
     */
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()

        when (command.value) {
            is Commands.Issue -> verifyIssueCommand(tx, command)
            is Commands.Transfer -> verifyTransferCommand(tx, command)
            is Commands.Settle -> verifySettleCommand(tx, command)
        }
    }

    private fun verifyIssueCommand(tx: LedgerTransaction, command: CommandWithParties<CommandData>) {
        requireThat {
            "No inputs should be consumed when issuing an IOU." using (tx.inputs.isEmpty())
            "Only one output state should be created when issuing an IOU." using (tx.outputs.size == 1)

            // IOU specific constraints
            val iou = tx.outputsOfType<IOUState>().single()
            "A newly issued IOU must have a positive amount." using (iou.amount.quantity > 0)
            "The lender and borrower cannot have the same identity." using (iou.borrower != iou.lender)

            // Constraints on signers
            "Both lender and borrower together only may sign IOU issue transaction." using (
                    checkSigners(command, setOf(iou.lender, iou.borrower))
                    )
        }
    }

    private fun verifyTransferCommand(tx: LedgerTransaction, command: CommandWithParties<CommandData>) {
        requireThat {
            "An IOU transfer transaction should only consume one input state." using (tx.inputs.size == 1)
            "An IOU transfer transaction should only create one output state." using (tx.outputs.size == 1)

            // IOU specific constraints
            val inputIOU = tx.inputsOfType<IOUState>().single()
            val outputIOU = tx.outputsOfType<IOUState>().single()
            "Only the lender property may change." using (inputIOU.copy(lender = outputIOU.lender) == outputIOU)
            "The lender property must change in a transfer." using (outputIOU.lender != inputIOU.lender)

            // Constraints on signers
            "The borrower, old lender and new lender only must sign an IOU transfer transaction" using (
                    checkSigners(command, setOf(inputIOU.lender, inputIOU.borrower, outputIOU.lender))
                    )
        }
    }

    private fun verifySettleCommand(tx: LedgerTransaction, command: CommandWithParties<CommandData>) {
        requireThat {
            // Input IOU specific constraints
            val iouStates = tx.groupStates { it: IOUState -> it.linearId }.single()
            // or val iouState = tx.groupStates(IOUState::class.java) { it.linearId }.single()
            "There must be one input IOU." using (iouStates.inputs.size == 1)


            //Cash specific constraints
            val cashStates = tx.outputsOfType<Cash.State>()
            "There must be output cash." using (cashStates.isNotEmpty())
            val inputIOU = iouStates.inputs.single()
            "Output cash must be paid to the lender." using (
                    cashStates.none { it.owner != inputIOU.lender }
                    )
            val remainingUnpaid = inputIOU.amount - inputIOU.paid
            val paidCash = cashStates.sumCash().withoutIssuer()
            "The amount settled cannot be more than the amount outstanding." using (paidCash <= remainingUnpaid)

            // Output IOU specific constraints
            val outputIOUs = iouStates.outputs
            if (paidCash < remainingUnpaid) {
                "There must be one output IOU." using (outputIOUs.size == 1)
                val outputIOU = outputIOUs.single()
                "Only the paid amount can change." using (inputIOU.copy(paid = outputIOU.paid) == outputIOU)
            } else {
                "There must be no output IOU as it has been fully settled." using (outputIOUs.isEmpty())
            }

            // Constraints on signers
            "Both lender and borrower together only must sign the IOU settle transaction." using (
                    checkSigners(command, setOf(inputIOU.lender, inputIOU.borrower))
                    )
        }
    }

    private fun checkSigners(command: CommandWithParties<CommandData>, parties: Set<Party>): Boolean {
        // Map always returns list so we need to run toSet again so the comparison works
        return command.signers.toSet() == parties.map { it.owningKey }.toSet()
    }
}