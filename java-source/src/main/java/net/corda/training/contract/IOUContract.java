package net.corda.training.contract;

import net.corda.core.contracts.*;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;

import net.corda.finance.contracts.asset.Cash;
import net.corda.training.state.IOUState;

import javax.swing.plaf.nimbus.State;
import java.lang.reflect.Array;
import java.security.PublicKey;
import java.util.*;
import java.util.stream.Collectors;

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
        class Issue extends TypeOnlyCommandData implements Commands{};
        class Transfer extends TypeOnlyCommandData implements Commands{};
        class Settle extends TypeOnlyCommandData implements Commands{};
    }
    /**
     * The contract code for the [IOUContract].
     * The constraints are self documenting so don't require any additional explanation.
     */
    @Override
    public void verify(LedgerTransaction tx) {

        CommandWithParties command = tx.getCommands().get(0);

        if (command.getValue() instanceof Commands.Issue) {

            requireThat(require -> {

                require.using("No inputs should be consumed when issuing an IOU.", tx.getInputStates().size() == 0);
                require.using( "Only one output state should be creatd when issuing an IOU.", tx.getOutputStates().size() == 0);

                IOUState outputState = (IOUState) tx.getOutput(0);
                require.using( "A newly issued IOU must have a positive amount.", outputState.amount.getQuantity() > 0);
                require.using( "The lender and borrower cannot have the same identity.", outputState.lender != outputState.borrower);

                List<Party> signingParties = tx.getCommands().get(0).getSigningParties();
                List<AbstractParty> participants = tx.getOutputStates().get(0).getParticipants();
                List<PublicKey> participantKeys = new ArrayList<>();

                for (AbstractParty party: participants) {
                    participantKeys.add(party.getOwningKey());
                }

                require.using("Both lender and borrower together only may sign IOU issue transaction.", signingParties.containsAll(participantKeys) && signingParties.size() == 2);

                return null;
            });

        }

        else if (command.getValue() instanceof Commands.Transfer) {

            requireThat(require -> {

                require.using("An IOU transfer transaction should only consume one input state.", tx.getInputStates().size() == 1);
                require.using("An IOU transfer transaction should only create one output state.", tx.getOutputStates().size() == 1);

                // Copy of input with new lender;
                IOUState inputState = (IOUState) tx.getInputStates().get(0);
                IOUState outputState = (IOUState) tx.getOutputStates().get(0);
                IOUState checkOutputState = outputState.withNewLender(inputState.getLender());


                require.using("Only the lender property may change.", checkOutputState.equals(inputState));
                require.using("The lender property must change in a transfer.", outputState.lender != inputState.lender);

                List<PublicKey> listOfPublicKeys = new ArrayList<>();
                listOfPublicKeys.add(inputState.lender.getOwningKey());
                listOfPublicKeys.add(inputState.borrower.getOwningKey());
                listOfPublicKeys.add(checkOutputState.lender.getOwningKey());

                require.using("The borrower, old lender and new lender only must sign an IOU transfer transaction", listOfPublicKeys.containsAll(command.getSigners()) && command.getSigners().size() == 3);

                return null;

            });

        }

        else if (command.getValue() instanceof Commands.Settle) {

            requireThat(require -> {

                // Check there is only one group of IOUs and that there is always an input IOU.
                List<LedgerTransaction.InOutGroup<IOUState, UniqueIdentifier>> groups = tx.groupStates(IOUState.class, IOUState::getLinearId);
                require.using("There must be one input IOU.", groups.get(0).getInputs().size() > 0);

                // Check that there are output cash states.
                List<Cash.State> allOutputCash = tx.outputsOfType(Cash.State.class);
                require.using("There must be output cash.", !allOutputCash.isEmpty());

                // Sum the cash states
                Amount<Currency> inputAmount = tx.inputsOfType(IOUState.class).get(0).amount;
                Amount<Currency> cashSum = new Amount<>(0, inputAmount.getToken());
                for (Cash.State cash: allOutputCash) {
                    Amount<Currency> addCash = new Amount<Currency>(cash.getAmount().getQuantity(), cash.getAmount().getToken().getProduct());
                    cashSum = cashSum.plus(addCash);
                }

                require.using("The amount settled cannot be more than the amount outstanding.", inputAmount.getQuantity() > cashSum.getQuantity());



                return null;
            });

        }

    }

//    // Check there is only one group of IOUs and that there is always an input IOU.
//    val ious = tx.groupStates<IOUState, UniqueIdentifier> { it.linearId }.single()
//    requireThat { "There must be one input IOU." using (ious.inputs.size == 1) }
//    // Check there are output cash states.
//    val cash = tx.outputsOfType<Cash.State>()
//    requireThat { "There must be output cash." using (cash.isNotEmpty()) }
//    // Check that the cash is being assigned to us.
//    val inputIou = ious.inputs.single()
//    val acceptableCash = cash.filter { it.owner == inputIou.lender }
//    requireThat { "There must be output cash paid to the recipient." using (acceptableCash.isNotEmpty()) }
//    // Sum the cash being sent to us (we don't care about the issuer).
//    val sumAcceptableCash = acceptableCash.sumCash().withoutIssuer()
//    val amountOutstanding = inputIou.amount - inputIou.paid
//    requireThat { "The amount settled cannot be more than the amount outstanding." using (amountOutstanding >= sumAcceptableCash) }
//    // Check to see if we need an output IOU or not.
//                if (amountOutstanding == sumAcceptableCash) {
//        // If the IOU has been fully settled then there should be no IOU output state.
//        requireThat { "There must be no output IOU as it has been fully settled." using (ious.outputs.isEmpty()) }
//    } else {
//        // If the IOU has been partially settled then it should still exist.
//        requireThat { "There must be one output IOU." using (ious.outputs.size == 1) }
//        // Check only the paid property changes.
//        val outputIou = ious.outputs.single()
//        requireThat {
//            "The amount may not change when settling." using (inputIou.amount == outputIou.amount)
//            "The borrower may not change when settling." using (inputIou.borrower == outputIou.borrower)
//            "The lender may not change when settling." using (inputIou.lender == outputIou.lender)
//        }
//    }
//    requireThat {
//        "Both lender and borrower together only must sign IOU settle transaction." using
//                (command.signers.toSet() == inputIou.participants.map { it.owningKey }.toSet())
//    }

}