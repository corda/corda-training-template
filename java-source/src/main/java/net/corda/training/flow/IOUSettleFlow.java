package net.corda.training.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import javafx.util.Pair;
import net.corda.confidential.IdentitySyncFlow;
import net.corda.core.contracts.*;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.finance.contracts.asset.Cash;
import net.corda.training.contract.IOUContract;
import net.corda.training.state.IOUState;
import java.lang.IllegalArgumentException;
import java.security.PublicKey;
import java.util.*;

import static net.corda.finance.contracts.GetBalances.getCashBalance;

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

        private final UniqueIdentifier stateLinearId;
        private final Amount<Currency> amount;

        public InitiatorFlow(UniqueIdentifier stateLinearId, Amount<Currency> amount) {
            this.stateLinearId = stateLinearId;
            this.amount = amount;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            // 1. Retrieve the IOU State from the vault.
            List<UUID> listOfLinearIds = new ArrayList<>();
            listOfLinearIds.add(stateLinearId.component2());
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, listOfLinearIds);

            Vault.Page results = getServiceHub().getVaultService().queryBy(IOUState.class, queryCriteria);
            StateAndRef inputStateAndRefToSettle = (StateAndRef) results.component1().get(0);
            IOUState inputStateToSettle = (IOUState) results.component1().get(0);

            // 2. Check the party running this flow is the borrower.
            if (inputStateToSettle.lender != getOurIdentity()) {
                throw new IllegalArgumentException("The borrower must issue the flow");
            }

            // 3. Create a transaction builder
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            TransactionBuilder tb = new TransactionBuilder(notary);

            // 4. Check we have enough cash to settle the requested amount
            final Amount<Currency> cashBalance = getCashBalance(getServiceHub(), (Currency) amount.getToken());

            if (cashBalance.getQuantity() < amount.getQuantity()) {
                throw new IllegalArgumentException("Borrower doesn't have enough cash to settle with the amount specified.");
            } else if (amount.getQuantity() > (inputStateToSettle.amount.getQuantity() - inputStateToSettle.paid.getQuantity())) {
                throw new IllegalArgumentException("Borrow tried to settle with more than was required for the obligation.");
            }

            // 5. Get some cash from the vault and add a spend to our transaction builder.
            List<PublicKey> cashKeys = Cash.generateSpend(getServiceHub(), tb, amount, inputStateToSettle.lender, ImmutableSet.of()).getSecond();


            // 6.
            Command<IOUContract.Commands.Settle> command = new Command<>(
                    new IOUContract.Commands.Settle(),
                    inputStateToSettle.getParticipants()
                            .stream().map(AbstractParty::getOwningKey)
                            .collect(Collectors.toList())
            );


            tb.addCommand(command);
            tb.addInputState(inputStateAndRefToSettle);

            // 7. Add an IOU output state for an IOU that has not been full settled.
            if (inputStateToSettle.amount.getQuantity() < amount.getQuantity()) {
                tb.addOutputState(inputStateToSettle.pay(amount), IOUContract.IOU_CONTRACT_ID);
            }

            // 8. Verify and sign the transaction
            tb.verify(getServiceHub());
            List<PublicKey> allKeysToSign = ImmutableList.of(getOurIdentity().getOwningKey());
            allKeysToSign.addAll(cashKeys);
            SignedTransaction ptx = getServiceHub().signInitialTransaction(tb, allKeysToSign);

            // 9. Collect other signatures
            List<FlowSession> counterPartyFlow = Arrays.asList(initiateFlow(inputStateToSettle.lender));
            SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, counterPartyFlow));

            //10. Finalize the transaction
            return subFlow(new FinalityFlow(stx));

        }

    }

    @InitiatedBy(IOUSettleFlow.InitiatorFlow.class)
    public static class ResponderFlow extends FlowLogic {

        private final FlowSession flowSession;

        public ResponderFlow(FlowSession flowSession) {
            this.flowSession = flowSession;
        }


        @Override
        public Object call() {

            return null;
        }
    }

}

///**
// * This is the flow which signs IOU settlements.
// * The signing is handled by the [SignTransactionFlow].
// */
//@InitiatedBy(IOUSettleFlow::class)
//class IOUSettleFlowResponder(val flowSession: FlowSession): FlowLogic<Unit>() {
//@Suspendable
//    override fun call() {
//
//            // Receiving information about anonymous identities
//            subFlow(IdentitySyncFlow.Receive(flowSession))
//
//            // signing transaction
//            val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
//            override fun checkTransaction(stx: SignedTransaction) {
//            }
//            }
//
//            subFlow(signedTransactionFlow)
//            }
//            }
//
///**
// * Self issues the calling node an amount of cash in the desired currency.
// * Only used for demo/sample/training purposes!
// */
//@InitiatingFlow
//@StartableByRPC
//class SelfIssueCashFlow(val amount: Amount<Currency>) : FlowLogic<Cash.State>() {
//@Suspendable
//    override fun call(): Cash.State {
//            /** Create the cash issue command. */
//            val issueRef = OpaqueBytes.of(0)
//            /** Note: ongoing work to support multiple notary identities is still in progress. */
//            val notary = serviceHub.networkMapCache.notaryIdentities.first()
//            /** Create the cash issuance transaction. */
//            val cashIssueTransaction = subFlow(CashIssueFlow(amount, issueRef, notary))
//            /** Return the cash output. */
//            return cashIssueTransaction.stx.tx.outputs.single().data as Cash.State
//            }
//            }