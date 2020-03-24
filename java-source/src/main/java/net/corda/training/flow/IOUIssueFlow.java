package net.corda.training.flow;

import co.paralleluniverse.fibers.Suspendable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import static net.corda.core.contracts.ContractsDSL.requireThat;
import net.corda.core.utilities.ProgressTracker;

import net.corda.training.contract.IOUContract;
import net.corda.training.state.IOUState;
import org.intellij.lang.annotations.Flow;

import javax.annotation.Signed;

import static net.corda.training.contract.IOUContract.Commands.*;

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

        public InitiatorFlow(IOUState state) {
        }

        // This is a mock function to prevent errors. Delete the body of the function before starting development.
        public SignedTransaction call() throws FlowException {
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            final TransactionBuilder builder = new TransactionBuilder(notary);
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);
			final List<FlowSession> sessions = Arrays.asList(initiateFlow(getOurIdentity()));
			return subFlow(new FinalityFlow(ptx, sessions));
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
				}
			}
			return subFlow(new SignTxFlow(flowSession, SignTransactionFlow.Companion.tracker()));
		}
	}
}