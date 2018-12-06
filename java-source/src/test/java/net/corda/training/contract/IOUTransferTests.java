package net.corda.training.contract;

import net.corda.core.contracts.*;
import net.corda.core.identity.AbstractParty;
import net.corda.finance.*;

import static net.corda.testing.node.NodeTestUtils.ledger;
import static net.corda.training.TestUtils.*;
import net.corda.testing.node.MockServices;
import net.corda.training.state.IOUState;
import org.junit.Test;
import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * Practical exercise instructions for Contracts Part 2.
 * The objective here is to write some contract code that verifies a transaction to issue an [IOUState].
 * As with the [IOUIssueTests] uncomment each unit test and run them one at a time. Use the body of the tests and the
 * task description to determine how to get the tests to pass.
 */

public class IOUTransferTests {

    // A dummy state
    ContractState DummyState = new ContractState() {
        @NotNull
        @Override
        public List<AbstractParty> getParticipants() {
            return null;
        }
    };

    // A dummy command
    CommandData DummyCommand = new CommandData() {
        @Override
        public int hashCode() {
            return super.hashCode();
        }
    };

     private final MockServices ledgerServices = new MockServices(Arrays.asList("net.corda.training"));

    /**tak
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

//     @Test
//     public void mustHandleMultipleCommandValues() {
//         IOUState iou = new IOUState(Currencies.DOLLARS(10), ALICE.getParty(), BOB.getParty());
//         ledger(ledgerServices, l -> {
//             l.transaction(tx -> {
//                 tx.output(IOUContract.class.getName(), iou);
//                 tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey()), DummyCommand);
//                 return tx.failsWith("Required net.corda.training.contract.IOUContract.Commands command");
//             });
//             l.transaction(tx -> {
//                 tx.output(IOUContract.class.getName(), iou);
//                 tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey()), IOUContract.Commands.Issue());
//                 return tx.verifies();
//             });
//             l.transaction(tx -> {
//                 tx.input(IOUContract.class.getName(), iou);
//                 tx.output(IOUContract.class.getName(), iou.withNewLender(CHARLIE.getParty()));
//                 tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey(), CHARLIE.getPublicKey()), IOUContract.Commands.Transfer());
//                 return tx.verifies();
//             });
//             return null;
//         });
//     }
}