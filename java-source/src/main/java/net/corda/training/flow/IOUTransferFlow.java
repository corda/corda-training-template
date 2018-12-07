package net.corda.training.flow;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.utilities.ProgressTracker;
import net.corda.training.contract.IOUContract.Commands.Transfer;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.training.contract.IOUContract;
import net.corda.training.state.IOUState;

import javax.annotation.Signed;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class IOUTransferFlow{

    @InitiatingFlow
    @StartableByRPC
    public static class InitiatorFlow extends FlowLogic<SignedTransaction> {
        private final UniqueIdentifier stateLinearId;
        private final Party newLender;

        public InitiatorFlow(UniqueIdentifier stateLinearId, Party newLender) {
            this.stateLinearId = stateLinearId;
            this.newLender = newLender;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            // Get query criteria
            List<UUID> listOfLinearIds = new ArrayList<>();
            listOfLinearIds.add(stateLinearId.component2());
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, listOfLinearIds);

            // Get flow components
            Vault.Page results = getServiceHub().getVaultService().queryBy(IOUState.class, queryCriteria);
            StateAndRef inputStateAndRefToTransfer = (StateAndRef) results.component1().get(0);
            IOUState inputStateToTransfer = (IOUState) results.component1().get(0);

            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            TransactionBuilder tb = new TransactionBuilder(notary);

            // Add command to the flow
            Command<Transfer> command = new Command<>(
                    new Transfer(),
                    inputStateToTransfer.getParticipants()
                    .stream().map(AbstractParty::getOwningKey)
                    .collect(Collectors.toList())
            );

            tb.addCommand(command);

            // Add states to flow
            tb.addInputState(inputStateAndRefToTransfer);
            tb.addOutputState(inputStateToTransfer.withNewLender(newLender), IOUContract.IOU_CONTRACT_ID);

            if (inputStateToTransfer.lender != getOurIdentity()) {
                throw new IllegalArgumentException("THis flow must be run by the current lender.");
            }


            // Verify the transaction
            tb.verify(getServiceHub());

            // Sign the transaction
            SignedTransaction partiallySignedTransaction = getServiceHub().signInitialTransaction(tb);

            //Collect Signatures
            List<FlowSession> listOfFlows = new ArrayList<>();

            for (AbstractParty participant: inputStateToTransfer.getParticipants()) {
                Party partyToInitiateFlow = (Party) participant;
                if (partyToInitiateFlow != getOurIdentity()) {
                    listOfFlows.add(initiateFlow(partyToInitiateFlow));
                }
            }

            listOfFlows.add(initiateFlow(newLender));

            SignedTransaction fullySignedTransaction = subFlow(new CollectSignaturesFlow(partiallySignedTransaction, listOfFlows));

            return subFlow(new FinalityFlow(fullySignedTransaction));
        }
    }


    /**
     * This is the flow which signs IOU settlements.
     * The signing is handled by the [SignTransactionFlow].
     */
    @InitiatedBy(IOUSettleFlow.InitiatorFlow.class)
    public static class Responder extends FlowLogic<SignedTransaction> {

        private final FlowSession otherPartyflow;

        public Responder(FlowSession otherPartyflow) {
            this.otherPartyflow = otherPartyflow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
                    super(otherPartyFlow, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using("This must be an IOU transaction", output instanceof IOUState);
                        return null;
                    });
                }
            }

            return subFlow(new SignTxFlow(otherPartyflow, SignTransactionFlow.Companion.tracker()));
        }

    }

}