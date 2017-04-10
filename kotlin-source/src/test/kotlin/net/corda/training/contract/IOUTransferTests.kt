package net.corda.training.contract

import net.corda.core.contracts.*
import net.corda.core.crypto.CompositeKey
import net.corda.core.utilities.DUMMY_PUBKEY_1
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
    // A pre-made IOU we will use for this exercise.
    val iou = IOUState(10.POUNDS, ALICE, BOB, IOUContract())
    // A pre-made dummy state we may need for some of the tests.
    class DummyState : ContractState {
        override val contract get() = DUMMY_PROGRAM_ID
        override val participants: List<CompositeKey> get() = listOf()
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
    @Test
    fun mustHandleMultipleCommandValues() {
        ledger {
            transaction {
                output { iou }
                command(ALICE_PUBKEY, BOB_PUBKEY) { DummyCommand() }
                this `fails with` "Required net.corda.training.contract.IOUContract.Commands command"
            }
            transaction {
                output { iou }
                command(ALICE_PUBKEY, BOB_PUBKEY) { IOUContract.Commands.Issue() }
                this.verifies()
            }
            transaction {
                input { iou }
                output { iou.withNewLender(CHARLIE) }
                command(ALICE_PUBKEY, BOB_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
                this.verifies()
            }
        }
    }

    /**
     * Task 2.
     * The transfer transactino should only have one input state and one output state.
     * TODO: Add constraints to the contract code to ensure a transfer transaction has only one input and output state.
     * Hint:
     * - Look at the contract code for "Issue".
     */
    @Test
    fun mustHaveOneInputAndOneOutput() {
        ledger {
            transaction {
                input { iou }
                input { DummyState() }
                output { iou.withNewLender(CHARLIE) }
                command(ALICE_PUBKEY, BOB_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
                this `fails with` "An IOU transfer transaction should only consume one input state."
            }
            transaction {
                output { iou }
                command(ALICE_PUBKEY, BOB_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
                this `fails with` "An IOU transfer transaction should only consume one input state."
            }
            transaction {
                input { iou }
                command(ALICE_PUBKEY, BOB_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
                this `fails with` "An IOU transfer transaction should only create one output state."
            }
            transaction {
                input { iou }
                output { iou.withNewLender(CHARLIE) }
                output { DummyState() }
                command(ALICE_PUBKEY, BOB_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
                this `fails with` "An IOU transfer transaction should only create one output state."
            }
            transaction {
                input { iou }
                output { iou.withNewLender(CHARLIE) }
                command(ALICE_PUBKEY, BOB_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
                this.verifies()
            }
        }
    }

    /**
     * Task 3.
     * Before you begin this task, add a new method to [IOUState] called [withoutLender]:
     *
     *     fun withoutLender() = copy(lender = Party("", NullPublicKey))
     *
     * This function creates a copy of a state with a NULL lender property, enabling to you easily check that all
     * properties in the [IOUState] are the same apart from the lender.
     * TODO: Add a constraint to the contract code to ensure only the lender property can change when transferring IOUs.
     * Hint:
     * - Use the [IOUState.withoutLender] method.
     * - You'll need references to the input and output ious.
     * - Remember you need to cast the [ContractState]s to [IOUState]s.
     * - It's easier to take this approach then check all properties other than the lender haven't changed, including
     *   the [linearId] and the [contract]!
     */
    @Test
    fun onlyTheLenderMayChange() {
        ledger {
            transaction {
                input { IOUState(10.DOLLARS, ALICE, BOB, IOUContract()) }
                output { IOUState(1.DOLLARS, ALICE, BOB, IOUContract()) }
                command(ALICE_PUBKEY, BOB_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
                this `fails with` "Only the lender property may change."
            }
            transaction {
                input { IOUState(10.DOLLARS, ALICE, BOB, IOUContract()) }
                output { IOUState(10.DOLLARS, ALICE, CHARLIE, IOUContract()) }
                command(ALICE_PUBKEY, BOB_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
                this `fails with` "Only the lender property may change."
            }
            transaction {
                input { IOUState(10.DOLLARS, ALICE, BOB, IOUContract(), 5.DOLLARS) }
                output { IOUState(10.DOLLARS, ALICE, BOB, IOUContract(), 10.DOLLARS) }
                command(ALICE_PUBKEY, BOB_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
                this `fails with` "Only the lender property may change."
            }
            transaction {
                input { iou }
                output { iou.withNewLender(CHARLIE) }
                command(ALICE_PUBKEY, BOB_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
                this.verifies()
            }
        }
    }

    /**
     * Task 4.
     * It is fairly obvious that in a transfer IOU transaction the lender must change.
     * TODO: Add a constraint to check the lender has changed in the output IOU.
     */
    @Test
    fun theLenderMustChange() {
        ledger {
            transaction {
                input { iou }
                output { iou }
                command(ALICE_PUBKEY, BOB_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
                this `fails with` "The lender property must change in a transfer."
            }
            transaction {
                input { iou }
                output { iou.withNewLender(CHARLIE) }
                command(ALICE_PUBKEY, BOB_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
                this.verifies()
            }
        }
    }

    /**
     * Task 5.
     * It is fairly obvious that in a transfer IOU transaction the lender must change!
     * TODO: Add a constraint to check the lender has changed in the output IOU.
     * Hint: The input sender cannot be the output sender!
     */
    @Test
    fun allParticipantsMustSign() {
        ledger {
            transaction {
                input { iou }
                output { iou.withNewLender(CHARLIE) }
                command(ALICE_PUBKEY, BOB_PUBKEY) { IOUContract.Commands.Transfer() }
                this `fails with` "The borrower, old lender and new lender only must sign an IOU transfer transaction"
            }
            transaction {
                input { iou }
                output { iou.withNewLender(CHARLIE) }
                command(ALICE_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
                this `fails with` "The borrower, old lender and new lender only must sign an IOU transfer transaction"
            }
            transaction {
                input { iou }
                output { iou.withNewLender(CHARLIE) }
                command(BOB_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
                this `fails with` "The borrower, old lender and new lender only must sign an IOU transfer transaction"
            }
            transaction {
                input { iou }
                output { iou.withNewLender(CHARLIE) }
                command(ALICE_PUBKEY, BOB_PUBKEY, MINI_CORP_PUBKEY) { IOUContract.Commands.Transfer() }
                this `fails with` "The borrower, old lender and new lender only must sign an IOU transfer transaction"
            }
            transaction {
                input { iou }
                output { iou.withNewLender(CHARLIE) }
                command(ALICE_PUBKEY, BOB_PUBKEY, CHARLIE_PUBKEY, MINI_CORP_PUBKEY) { IOUContract.Commands.Transfer() }
                this `fails with` "The borrower, old lender and new lender only must sign an IOU transfer transaction"
            }
            transaction {
                input { iou }
                output { iou.withNewLender(CHARLIE) }
                command(ALICE_PUBKEY, BOB_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Transfer() }
                this.verifies()
            }
        }
    }
}