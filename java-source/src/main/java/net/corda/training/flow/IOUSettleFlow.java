package net.corda.training.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import kotlin.Pair;
import net.corda.core.contracts.*;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.identity.PartyAndCertificate;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.core.utilities.ProgressTracker;
import net.corda.finance.contracts.asset.Cash;
import net.corda.finance.contracts.asset.PartyAndAmount;
import net.corda.finance.flows.AbstractCashFlow;
import net.corda.finance.flows.CashIssueFlow;
import net.corda.finance.workflows.asset.CashUtils;
import net.corda.training.contract.IOUContract;
import net.corda.training.state.IOUState;

import java.lang.IllegalArgumentException;
import java.security.PublicKey;
import java.util.*;

import static net.corda.core.contracts.ContractsDSL.requireThat;
import static net.corda.finance.workflows.GetBalances.getCashBalance;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class IOUSettleFlow {

    /**
     * This is the flow which handles the (partial) settlement of existing IOUs on the ledger.
     * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
     * Notarisation (if required) and commitment to the ledger is handled vy the [FinalityFlow].
     * The flow returns the [SignedTransaction] that was committed to the ledger.
     */
    @InitiatingFlow
    @StartableByRPC
    public static class InitiatorFlow extends FlowLogic<SignedTransaction> {

        private final UniqueIdentifier linearId;
        private final Amount<Currency> amount;

        public InitiatorFlow(UniqueIdentifier linearId, Amount<Currency> amount) {
            this.linearId = linearId;
            this.amount = amount;
        }

        @Suspendable
        public SignedTransaction call() throws FlowException {

            // Task 1
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, Arrays.asList(linearId.getId()));
            Vault.Page<IOUState> query = getServiceHub().getVaultService().queryBy(IOUState.class, queryCriteria);
            IOUState iouState = query.getStates().get(0).getState().getData();

            // Task 2
            if (!iouState.getBorrower().equals(getOurIdentity())) throw new IllegalArgumentException("The borrower must issue the flow.");

            // Task 3
            Amount<Currency> cashBalance = getCashBalance(getServiceHub(), amount.getToken());
            if (cashBalance.getQuantity() <= 0) {
                throw new IllegalArgumentException("Borrower has no " + amount.getToken().getSymbol() + " to settle.");
            }

            // Task 4
            if (cashBalance.getQuantity() < amount.getQuantity()) {
                throw new IllegalArgumentException("Borrower doesn't have enough cash to settle with the amount specified.");
            }

            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            final TransactionBuilder transactionBuilder = new TransactionBuilder(notary);
            final PartyAndCertificate ourIdentity = getOurIdentityAndCert();
            final AbstractParty recipientParty = iouState.getLender();
            final Set<AbstractParty> spendingParties = new HashSet<>(iouState.getParticipants());

            CashUtils.generateSpend(getServiceHub(), transactionBuilder, amount, ourIdentity, recipientParty, spendingParties);

            List<PublicKey> requiredKeys = Arrays.asList(iouState.getLender().getOwningKey(), iouState.getBorrower().getOwningKey());
            final Command<IOUContract.Commands.Settle> settleCommand = new Command<>(new IOUContract.Commands.Settle(), requiredKeys);

            IOUState outputState = iouState.pay(amount);

            transactionBuilder.addCommand(settleCommand);
            transactionBuilder.addInputState(query.component1().get(0));
            transactionBuilder.addOutputState(outputState);
            transactionBuilder.verify(getServiceHub());

            SignedTransaction partiallySignedTransaction = getServiceHub().signInitialTransaction(transactionBuilder, getOurIdentity().getOwningKey());

            // Task 5
            Set<Party> participants = Sets.newHashSet(iouState.getLender());
            Set<FlowSession> flowSessions = participants.stream().map(this::initiateFlow).collect(Collectors.toSet());
            SignedTransaction fullySignedTransaction = subFlow(new CollectSignaturesFlow(partiallySignedTransaction, flowSessions));

            return subFlow(new FinalityFlow(fullySignedTransaction, flowSessions));
        }
    }

    /**
     * This is the flow which signs IOU settlements.
     * The signing is handled by the [SignTransactionFlow].
     * Uncomment the initiatedBy annotation to facilitate the responder flow.
     */
    @InitiatedBy(IOUSettleFlow.InitiatorFlow.class)
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
                protected void checkTransaction(SignedTransaction stx) {
                    requireThat(require -> {
                        ContractState output = stx.getTx().outputsOfType(IOUState.class).get(0);
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

    /**
     * Self issues the calling node an amount of cash in the desired currency.
     * Only used for demo/sample/training purposes!
     */

    @InitiatingFlow
    @StartableByRPC
    public static class SelfIssueCashFlow extends FlowLogic<Cash.State> {

        Amount<Currency> amount;

        SelfIssueCashFlow(Amount<Currency> amount) {
            this.amount = amount;
        }

        @Suspendable
        @Override
        public Cash.State call() throws FlowException {
            // Create the cash issue command.
            OpaqueBytes issueRef = OpaqueBytes.of(new byte[0]);
            // Note: ongoing work to support multiple notary identities is still in progress. */
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            // Create the cash issuance transaction.
            AbstractCashFlow.Result cashIssueTransaction = subFlow(new CashIssueFlow(amount, issueRef, notary));
            return (Cash.State) cashIssueTransaction.getStx().getTx().getOutput(0);
        }

    }

}