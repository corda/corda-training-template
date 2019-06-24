package net.corda.training.flow;

import co.paralleluniverse.fibers.Suspendable;

import java.util.Set;
import java.util.stream.Collectors;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import static net.corda.core.contracts.ContractsDSL.requireThat;
import net.corda.core.utilities.ProgressTracker;

import net.corda.training.contract.IOUContract;
import net.corda.training.contract.IOUContract.Commands.Issue;
import net.corda.training.state.IOUState;

/**
 * This is the flow which handles issuance of new IOUs on the ledger.
 * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
 * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
 * The flow returns the [SignedTransaction] that was committed to the ledger.
 */
public class IOUIssueFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class InitiatorFlow extends FlowLogic<SignedTransaction> {

    	private final IOUState state;
        public InitiatorFlow(IOUState state) {
        	this.state = state;
        }

        @Suspendable
		public SignedTransaction call() throws FlowException {

        	// Task 1
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            final TransactionBuilder transactionBuilder = new TransactionBuilder(notary);
			final Command<Issue> issueCommand = new Command<>(new Issue(), state.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));
			transactionBuilder.addCommand(issueCommand);
			transactionBuilder.addOutputState(state, IOUContract.IOU_CONTRACT_ID);

			// Task 2
			transactionBuilder.verify(getServiceHub());
			SignedTransaction partiallySignedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);
			
			// Task 3
			Set<Party> participants = state.getParticipants().stream().map(p -> (Party)p).collect(Collectors.toSet());
			participants.remove(getOurIdentity());
			Set<FlowSession> flowSessions = participants.stream().map(this::initiateFlow).collect(Collectors.toSet());
			SignedTransaction signedTransaction = subFlow(new CollectSignaturesFlow(partiallySignedTransaction, flowSessions));

			// Task 4
			return subFlow(new FinalityFlow(signedTransaction, flowSessions));
        }
    }

	/**
	 * This is the flow which signs IOU issuances.
	 * The signing is handled by the [SignTransactionFlow].
     * Uncomment the initiatedBy annotation to facilitate the responder flow.
	 */

	@InitiatedBy(IOUIssueFlow.InitiatorFlow.class)
	public static class ResponderFlow extends FlowLogic<SignedTransaction>{

		private final FlowSession flowSession;
		private SecureHash txWeJustSigned;

		public ResponderFlow(FlowSession flowSession){
			this.flowSession = flowSession;
		}

		@Suspendable
		@Override
		public SignedTransaction call() throws FlowException {
			class SignTxFlow extends SignTransactionFlow{

				private SignTxFlow(FlowSession flowSession, ProgressTracker progressTracker){
					super(flowSession, progressTracker);
				}

				@Override
				protected void checkTransaction(SignedTransaction stx){
					requireThat(req -> {
						ContractState output = stx.getTx().getOutputs().get(0).getData();
						req.using("This must be an IOU transaction", output instanceof IOUState);
						return null;
					});
					txWeJustSigned = stx.getId();
				}
			}

			flowSession.getCounterpartyFlowInfo().getFlowVersion();

			// Create a sign transaction flow
			SignTxFlow signTxFlow = new SignTxFlow(flowSession, SignTransactionFlow.Companion.tracker());

			// Run the sign transaction flow to sign the transaction
			subFlow(signTxFlow);

			// Run the ReceiveFinalityFlow to finalize the transaction and persist it to the vault.
			return subFlow(new ReceiveFinalityFlow(flowSession, txWeJustSigned));
		}
	}
}