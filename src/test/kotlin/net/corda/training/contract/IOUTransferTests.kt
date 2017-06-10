package net.corda.training.contract

import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.utilities.ALICE
import net.corda.core.utilities.BOB
import net.corda.core.utilities.CHARLIE
import net.corda.testing.*
import net.corda.training.state.IOUState
import org.junit.Test


/**
 * Practical exercise instructions.
 * The objective here is to write some contract code that verifies a transaction to issue an [IOUState].
 * As with the [IOUIssueTests] uncomment each unit test and run them one at a time. Use the body of the tests and the
 * task description to determine how to get the tests to pass.
 */
class IOUTransferTests {
    // A pre-made dummy state we may need for some of the tests.
    class DummyState : ContractState {
        override val contract get() = DUMMY_PROGRAM_ID
        override val participants: List<AbstractParty> get() = listOf()
    }
    // A dummy command.
    class DummyCommand : CommandData

    /**
     * Task 1.
     * Now things are going to get interesting!
     * We need the [IOUContract] to not only handle Issues of IOUs but now also Transfers.
     * Of course, we'll need to add a new Command and add some additional contract code to handle Transfers.
     * TODO: Add a "Transfer" command to the IOUState and update the verify() function to handle multiple commands.
     * Hint:
     * - As with the [Issue] command, add the [Transfer] command within the [IOUContract.Commands].
     * - Again, we only care about the existence of the [Transfer] command in a transaction, therefore it should
     *   subclass the [TypeOnlyCommandData].
     * - You can use the [requireSingleCommand] function to check for the existence of a command which implements a
     *   specified interface. Instead of using
     *
     *       tx.commands.requireSingleCommand<Commands.Issue>()
     *
     *   You can instead use:
     *
     *       tx.commands.requireSingleCommand<Commands>()
     *
     *   To match any command that implements [IOUContract.Commands]
     * - We then need to switch on the type of [Command.value], in Kotlin you can do this using a "when" block
     * - For each "when" block case, you can check the type of [Command.value] using the "is" keyword:
     *
     *       val command = ...
     *       when (command.value) {
     *           is Commands.X -> doSomething()
     *           is Commands.Y -> doSomethingElse()
     *       }
     * - The [requireSingleCommand] function will handle unrecognised types for you (see first unit test).
     */
//    @Test
//    fun mustHandleMultipleCommandValues() {
//        val iou = IOUState(10.POUNDS, ALICE, BOB)
//        ledger {
//            transaction {
//                output { iou }
//                command(ALICE_PUBKEY, BOB_PUBKEY) { DummyCommand() }
//                this `fails with` "Required net.corda.training.contract.IOUContract.Commands command"
//            }
//            transaction {
//                output { iou }
//                command(ALICE_PUBKEY, BOB_PUBKEY) { IOUContract.Commands.Issue() }
//                this.verifies()
//            }
//            transaction {
//                input { iou }
//                output { iou.withNewLender(CHARLIE) }
//                command(ALICE_PUBKEY, BOB_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
//                this.verifies()
//            }
//        }
//    }

    /**
     * Task 2.
     * The transfer transaction should only have one input state and one output state.
     * TODO: Add constraints to the contract code to ensure a transfer transaction has only one input and output state.
     * Hint:
     * - Look at the contract code for "Issue".
     */
//    @Test
//    fun mustHaveOneInputAndOneOutput() {
//        val iou = IOUState(10.POUNDS, ALICE, BOB)
//        ledger {
//            transaction {
//                input { iou }
//                input { DummyState() }
//                output { iou.withNewLender(CHARLIE) }
//                command(ALICE_PUBKEY, BOB_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
//                this `fails with` "An IOU transfer transaction should only consume one input state."
//            }
//            transaction {
//                output { iou }
//                command(ALICE_PUBKEY, BOB_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
//                this `fails with` "An IOU transfer transaction should only consume one input state."
//            }
//            transaction {
//                input { iou }
//                command(ALICE_PUBKEY, BOB_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
//                this `fails with` "An IOU transfer transaction should only create one output state."
//            }
//            transaction {
//                input { iou }
//                output { iou.withNewLender(CHARLIE) }
//                output { DummyState() }
//                command(ALICE_PUBKEY, BOB_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
//                this `fails with` "An IOU transfer transaction should only create one output state."
//            }
//            transaction {
//                input { iou }
//                output { iou.withNewLender(CHARLIE) }
//                command(ALICE_PUBKEY, BOB_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
//                this.verifies()
//            }
//        }
//    }

    /**
     * Task 3.
     * TODO: Add a constraint to the contract code to ensure only the lender property can change when transferring IOUs.
     * Hint:
     * - You can use the [IOUState.copy] method.
     * - You can compare a copy of the input to the output with the lender of the output as the lender of the input.
     * - You'll need references to the input and output ious.
     * - Remember you need to cast the [ContractState]s to [IOUState]s.
     * - It's easier to take this approach then check all properties other than the lender haven't changed, including
     *   the [linearId] and the [contract]!
     */
//    @Test
//    fun onlyTheLenderMayChange() {
//        val iou = IOUState(10.POUNDS, ALICE, BOB)
//        ledger {
//            transaction {
//                input { IOUState(10.DOLLARS, ALICE, BOB) }
//                output { IOUState(1.DOLLARS, ALICE, BOB) }
//                command(ALICE_PUBKEY, BOB_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
//                this `fails with` "Only the lender property may change."
//            }
//            transaction {
//                input { IOUState(10.DOLLARS, ALICE, BOB) }
//                output { IOUState(10.DOLLARS, ALICE, CHARLIE) }
//                command(ALICE_PUBKEY, BOB_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
//                this `fails with` "Only the lender property may change."
//            }
//            transaction {
//                input { IOUState(10.DOLLARS, ALICE, BOB, 5.DOLLARS) }
//                output { IOUState(10.DOLLARS, ALICE, BOB, 10.DOLLARS) }
//                command(ALICE_PUBKEY, BOB_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
//                this `fails with` "Only the lender property may change."
//            }
//            transaction {
//                input { iou }
//                output { iou.withNewLender(CHARLIE) }
//                command(ALICE_PUBKEY, BOB_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
//                this.verifies()
//            }
//        }
//    }

    /**
     * Task 4.
     * It is fairly obvious that in a transfer IOU transaction the lender must change.
     * TODO: Add a constraint to check the lender has changed in the output IOU.
     */
//    @Test
//    fun theLenderMustChange() {
//        val iou = IOUState(10.POUNDS, ALICE, BOB)
//        ledger {
//            transaction {
//                input { iou }
//                output { iou }
//                command(ALICE_PUBKEY, BOB_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
//                this `fails with` "The lender property must change in a transfer."
//            }
//            transaction {
//                input { iou }
//                output { iou.withNewLender(CHARLIE) }
//                command(ALICE_PUBKEY, BOB_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
//                this.verifies()
//            }
//        }
//    }

    /**
     * Task 5.
     * All the participants in a transfer IOU transaction must sign.
     * TODO: Add a constraint to check the old lender, the new lender and the recipient have signed.
     */
//    @Test
//    fun allParticipantsMustSign() {
//        val iou = IOUState(10.POUNDS, ALICE, BOB)
//        ledger {
//            transaction {
//                input { iou }
//                output { iou.withNewLender(CHARLIE) }
//                command(ALICE_PUBKEY, BOB_PUBKEY) { IOUContract.Commands.Transfer() }
//                this `fails with` "The borrower, old lender and new lender only must sign an IOU transfer transaction"
//            }
//            transaction {
//                input { iou }
//                output { iou.withNewLender(CHARLIE) }
//                command(ALICE_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
//                this `fails with` "The borrower, old lender and new lender only must sign an IOU transfer transaction"
//            }
//            transaction {
//                input { iou }
//                output { iou.withNewLender(CHARLIE) }
//                command(BOB_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
//                this `fails with` "The borrower, old lender and new lender only must sign an IOU transfer transaction"
//            }
//            transaction {
//                input { iou }
//                output { iou.withNewLender(CHARLIE) }
//                command(ALICE_PUBKEY, BOB_PUBKEY, MINI_CORP_PUBKEY) { IOUContract.Commands.Transfer() }
//                this `fails with` "The borrower, old lender and new lender only must sign an IOU transfer transaction"
//            }
//            transaction {
//                input { iou }
//                output { iou.withNewLender(CHARLIE) }
//                command(ALICE_PUBKEY, BOB_PUBKEY, CHARLIE_PUBKEY, MINI_CORP_PUBKEY) { IOUContract.Commands.Transfer() }
//                this `fails with` "The borrower, old lender and new lender only must sign an IOU transfer transaction"
//            }
//            transaction {
//                input { iou }
//                output { iou.withNewLender(CHARLIE) }
//                command(ALICE_PUBKEY, BOB_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
//                this.verifies()
//            }
//        }
//    }
}