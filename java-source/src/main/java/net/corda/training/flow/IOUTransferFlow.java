package net.corda.training.flow;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.node.services.vault.Builder;
import net.corda.core.node.services.vault.CriteriaExpression;
import net.corda.core.utilities.ProgressTracker;
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
import javax.validation.constraints.NotNull;
import java.security.PublicKey;
import java.util.*;
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
        public SignedTransaction call() throws FlowException {

            // Task 1

            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, new ArrayList<>(Arrays.asList(stateLinearId.getId())));
            Vault.Page<IOUState> query = getServiceHub().getVaultService().queryBy(IOUState.class, queryCriteria);
            IOUState iouState = query.getStates().get(0).getState().getData();

            IOUState iouStateCopy = iouState.withNewLender(newLender);

            List<PublicKey> requiredKeys = new ArrayList<>();
            requiredKeys.addAll(iouState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));
            requiredKeys.add(newLender.getOwningKey());

            final Command<IOUContract.Commands.Transfer> transferCommand = new Command<>(new IOUContract.Commands.Transfer(), requiredKeys);

            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            final TransactionBuilder transactionBuilder = new TransactionBuilder(notary);
            transactionBuilder.addCommand(transferCommand);
            StateAndRef stateAndRef = query.getStates().get(0);
            transactionBuilder.addInputState(stateAndRef);
            transactionBuilder.addOutputState(iouStateCopy);

            transactionBuilder.verify(getServiceHub());
            SignedTransaction partiallySignedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);

            // Task 2
            if (!getOurIdentity().equals(iouState.getLender())) throw new IllegalArgumentException("This flow must be run by the current lender.");

            // Task 3

            // Task 4
            Set<Party> participants = new HashSet<>();
            participants.add(iouState.getBorrower());
            participants.add(newLender);
            Set<FlowSession> flowSessions = participants.stream().map(this::initiateFlow).collect(Collectors.toSet());
            SignedTransaction signedTransaction = subFlow(new CollectSignaturesFlow(partiallySignedTransaction, flowSessions));

            // Task 5
            return subFlow(new FinalityFlow(signedTransaction, flowSessions));
        }
    }


    /**
     * This is the flow which signs IOU settlements.
     * The signing is handled by the [SignTransactionFlow].
     * Uncomment the initiatedBy annotation to facilitate the responder flow.
     */
    @InitiatedBy(IOUTransferFlow.InitiatorFlow.class)
    public static class Responder extends FlowLogic<SignedTransaction> {

        private final FlowSession otherPartyFlow;
        private SecureHash txWeJustSigned;
        public Responder(FlowSession otherPartyFlow) {
            this.otherPartyFlow = otherPartyFlow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
                    super(otherPartyFlow, progressTracker);
                }
                @Override
                @NotNull
                protected void checkTransaction(SignedTransaction stx) {
                    requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using("This must be an IOU transaction", output instanceof IOUState);
                        return null;
                    });
                    txWeJustSigned = stx.getId();
                }
            }

            SignTxFlow signTxFlow = new SignTxFlow(otherPartyFlow, SignTxFlow.tracker());

            subFlow(signTxFlow);

            return subFlow(new ReceiveFinalityFlow(otherPartyFlow, txWeJustSigned));
        }

    }

}