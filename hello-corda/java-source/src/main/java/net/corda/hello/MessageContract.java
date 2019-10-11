package net.corda.hello;

import net.corda.core.contracts.*;
import net.corda.core.transactions.LedgerTransaction;
import java.util.Collections;
import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;
import net.corda.hello.MessageContract.Commands.SendMessage;

/** TODO(#3):
 * Add an additional requirement to this contract so that the 'content' variable in a MessageState cannot be an empty String.
 * Make sure the message in the require statement is as follows "The content cannot be an empty String.".
 **/

public class MessageContract implements Contract {
    public static final String ID = "net.corda.hello.MessageContract";
    public interface Commands extends CommandData {
        class SendMessage extends TypeOnlyCommandData implements Commands{}
    }
    @Override
    public void verify(LedgerTransaction tx) {
        final CommandWithParties<SendMessage> command = requireSingleCommand(tx.getCommands(), SendMessage.class);
        requireThat( require -> {
            require.using("There should be no input state.", tx.getInputStates().isEmpty());
            require.using("There should one output state.", tx.getOutputStates().size() == 1);
            final MessageState outputState = tx.outputsOfType(MessageState.class).get(0);
            require.using("The party sending the message must sign the SendMessage transaction.",
                    (command.getSigners().equals(Collections.singletonList(outputState.origin.getOwningKey()))));
            return null;
        });
    }
}