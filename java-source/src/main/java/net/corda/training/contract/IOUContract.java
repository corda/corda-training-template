package net.corda.training.contract;

import net.corda.core.contracts.*;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;

import net.corda.finance.contracts.asset.Cash;

import net.corda.training.state.IOUState;

import java.security.PublicKey;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is where you'll add the contract code which defines how the [IOUState] behaves. Looks at the unit tests in
 * [IOUContractTests] for instructions on how to complete the [IOUContract] class.
 */
public class IOUContract implements Contract {
    public static final String IOU_CONTRACT_ID = "net.corda.training.contract.IOUContract";

    /**
     * Add any commands required for this contract as classes within this interface.
     * It is useful to encapsulate your commands inside an interface, so you can use the [requireSingleCommand]
     * function to check for a number of commands which implement this interface.
     */
    public interface Commands extends CommandData {
        class Issue extends TypeOnlyCommandData implements Commands {}
        class Transfer extends TypeOnlyCommandData implements Commands {}
        class Settle extends TypeOnlyCommandData implements Commands {}
    }

    /**
     * The contract code for the [IOUContract].
     * The constraints are self documenting so don't require any additional explanation.
     */
    @Override
    public void verify(LedgerTransaction tx) {

        final CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);
        final Commands commandData = command.getValue();

        if (commandData instanceof Commands.Issue) {
            requireThat( req -> {

                // Task 2
                req.using("No inputs should be consumed when issuing an IOU", tx.getInputs().size() == 0);

                // Task 3
                req.using("Only one output state should be created when issuing an IOU.", tx.getOutputs().size() == 1);

                // Task 4
                final IOUState outputState = (IOUState)tx.getOutputStates().get(0);
                req.using("A newly issued IOU must have a positive amount.", outputState.getAmount().getQuantity() > 0);

                // Task 5
                req.using("The lender and borrower cannot have the same identity.", outputState.getLender() != outputState.getBorrower());

                // Task 6
                final List<AbstractParty> participants = outputState.getParticipants();
                final Set<PublicKey> participantKeys = participants.stream().map(element -> (element.getOwningKey())).collect(Collectors.toSet());
                final Set<PublicKey> signers = command.getSigners().stream().collect(Collectors.toSet());
                req.using("Both lender and borrower together only may sign IOU issue transaction.", participantKeys.equals(signers));

                return null;
            });
        }
        else if (commandData instanceof Commands.Transfer) {
            requireThat( req -> {

                // Task 2
                req.using("An IOU transfer transaction should only consume one input state.", tx.getInputs().size() == 1);
                req.using("An IOU transfer transaction should only create one output state.", tx.getOutputs().size() == 1);

                // Task 3
                final IOUState inputState = (IOUState) tx.getInput(0);
                final IOUState outputState = (IOUState) tx.getOutput(0);
                final IOUState inputStateCopy = inputState.copy(inputState.getAmount(), outputState.getLender(), inputState.getBorrower(), inputState.getPaid());
                req.using("Only the lender property may change.", inputStateCopy.equals(outputState));

                // Task 4
                req.using("The lender property must change in a transfer.", !inputState.getLender().equals(outputState.getLender()));

                // Task 5
                final List<AbstractParty> participants = outputState.getParticipants();
                final Set<PublicKey> participantKeys = participants.stream().map(AbstractParty::getOwningKey).collect(Collectors.toSet());
                participantKeys.add(inputState.getLender().getOwningKey());
                final Set<PublicKey> signers = command.getSigners().stream().collect(Collectors.toSet());
                req.using("The borrower, old lender and new lender only must sign an IOU transfer transaction", participantKeys.equals(signers));

                return null;
            });
        }
        else if (commandData instanceof Commands.Settle) {
            requireThat( req -> {

                // Task 2
                final List<LedgerTransaction.InOutGroup<IOUState, UniqueIdentifier>> groupStates = tx.groupStates(IOUState.class, IOUState::getLinearId);
                req.using("List has more than one element.", groupStates.size() == 1);

                // Task 3
                req.using("There must be one input IOU.", groupStates.get(0).getInputs().size() == 1);

                // Task 4
                req.using("There must be output cash.", tx.outputsOfType(Cash.State.class).size() > 0);

                // Task 5
                final Party lenderInput = groupStates.get(0).getInputs().get(0).getLender();
                final long numCashStates = tx.outputsOfType(Cash.State.class).stream().filter(e -> e.getOwner().equals(lenderInput)).count();
                req.using("There must be output cash paid to the recipient.", numCashStates > 0);

                // Task 6
                final Amount<Currency> outstandingAmount = groupStates.get(0).getInputs().get(0).getAmount();
                Amount<Currency> sumAmount = new Amount<>(0, outstandingAmount.component2(), outstandingAmount.component3());
                List<Cash.State> outputCash = tx.outputsOfType(Cash.State.class).stream().filter(e -> e.getOwner().equals(lenderInput)).collect(Collectors.toList());
                for (Cash.State c : outputCash) {
                    sumAmount = sumAmount.plus(new Amount<>(c.getAmount().getQuantity(), c.getAmount().getDisplayTokenSize(), c.getAmount().getToken().component2()));
                }
                req.using("The amount settled cannot be more than the amount outstanding.", outstandingAmount.compareTo(sumAmount) >= 0 ); //sumAmount <= outstandingAmount

                // Task 7


                // Task 8
                if (sumAmount.getQuantity() == outstandingAmount.getQuantity()) { // (fully settled)
                    req.using("There must be no output IOU as it has been fully settled.", tx.outputsOfType(IOUState.class).size() == 0);
                }
                else { // sum < outstanding (partially settled)
                    req.using("There must be one output IOU.", tx.outputsOfType(IOUState.class).size() == 1);

                    // Task 9
                    final IOUState outputState = tx.outputsOfType(IOUState.class).get(0);
                    final IOUState inputState = tx.inputsOfType(IOUState.class).get(0);
                    req.using("The borrower may not change when settling.", outputState.getBorrower().equals(inputState.getBorrower()));
                    req.using("The amount may not change when settling.", outputState.getAmount().equals(inputState.getAmount()));
                    req.using("The lender may not change when settling.", outputState.getLender().equals(inputState.getLender()));

                }

                // Task 10
                final IOUState inputState = tx.inputsOfType(IOUState.class).get(0);
                final Set<PublicKey> signers = new HashSet<>(command.getSigners());
                final Set<PublicKey> participantKeys = inputState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toSet());
                req.using("Both lender and borrower together only must sign IOU settle transaction.", signers.equals(participantKeys));

                return null;
            });

        }

    }
}