package net.corda.training.flow;

import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandAndState;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.flows.FlowSession;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.finance.schemas.CashSchemaV1;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.core.transactions.SignedTransaction;
import net.corda.finance.Currencies;
import net.corda.finance.contracts.asset.Cash;
import net.corda.testing.node.*;
import net.corda.training.contract.IOUContract;
import net.corda.training.contract.IOUIssueTests;
import net.corda.training.state.IOUState;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static net.corda.testing.driver.Driver.driver;
import static net.corda.testing.node.NodeTestUtils.ledger;
import static net.corda.training.TestUtils.ALICE;
import static net.corda.training.TestUtils.BOB;

public class IOUSettleFlowTests{

    private MockNetwork mockNetwork;
    private StartedMockNode a, b, c;

    @Before
    public void setup() {

        MockNetworkParameters mockNetworkParameters = new MockNetworkParameters().withNotarySpecs(Arrays.asList(new MockNetworkNotarySpec(new CordaX500Name("Notary", "London", "GB"))));
        mockNetwork = new MockNetwork(Arrays.asList("net.corda.training", "net.corda.finance.contracts.asset", "net.corda.finance.schemas"), mockNetworkParameters);
        System.out.println(mockNetwork);

        a = mockNetwork.createNode(new MockNodeParameters());
        b = mockNetwork.createNode(new MockNodeParameters());
        c = mockNetwork.createNode(new MockNodeParameters());

        ArrayList<StartedMockNode> startedNodes = new ArrayList<>();
        startedNodes.add(a);
        startedNodes.add(b);
        startedNodes.add(c);

        // For real nodes this happens automatically, but we have to manually register the flow for tests
        startedNodes.forEach(el -> el.registerInitiatedFlow(IOUIssueFlow.ResponderFlow.class));
        mockNetwork.runNetwork();
    }

    @After
    public void tearDown() {
        mockNetwork.stopNodes();
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private SignedTransaction issueIOU(IOUState iouState) throws InterruptedException, ExecutionException {
        IOUIssueFlow.InitiatorFlow flow = new IOUIssueFlow.InitiatorFlow(iouState);
        CordaFuture future = a.startFlow(flow);
        mockNetwork.runNetwork();
        return (SignedTransaction) future.get();
    }

    private Cash.State issueCash(Amount<Currency> amount) throws InterruptedException, ExecutionException {
        SelfIssueCashFlow flow = new SelfIssueCashFlow(amount);
        CordaFuture future = a.startFlow(flow);
        mockNetwork.runNetwork();
        return (Cash.State) future.get();
    }

    /**
     * Task 1.
     * The first task is to grab the [IOUState] for the given [linearId] from the vault, assemble a transaction
     * and sign it.
     * TODO: Grab the IOU for the given [linearId] from the vault, build and sign the settle transaction.
     * Hints:
     * - Use the code from the [IOUTransferFlow] to get the correct [IOUState] from the vault.
     * - You will need to use the [Cash.generateSpend] functionality of the vault to add the cash states and cash command
     *   to your transaction. The API is quite simple. It takes a reference to a [TransactionBuilder], an [Amount] and
     *   the [Party] object for the recipient. The function will mutate your builder by adding the states and commands.
     * - You then need to produce the output [IOUState] by using the [IOUState.pay] function.
     * - Add the input [IOUState] [StateAndRef] and the new output [IOUState] to the transaction.
     * - Sign the transaction and return it.
     */

    @Test
    public void flowReturnsCorrectlyFormedPartiallySignedTransaction() throws Exception {
        SignedTransaction stx = issueIOU(new IOUState(Currencies.POUNDS(10), b.getInfo().getLegalIdentities().get(0), a.getInfo().getLegalIdentities().get(0)));
        issueCash(Currencies.POUNDS(5));
        IOUState inputIOU = stx.getTx().outputsOfType(IOUState.class).get(0);
        IOUSettleFlow.InitiatorFlow flow = new IOUSettleFlow.InitiatorFlow(inputIOU.getLinearId(), Currencies.POUNDS(5));
        SignedTransaction settleResult = a.startFlow(flow).get();

        mockNetwork.runNetwork();

        // Check the transaction is well formed...
        // One output IOUState, one input IOUState reference, input and output cash
        a.transaction(() -> {
            try {
                LedgerTransaction ledgerTx = settleResult.toLedgerTransaction(a.getServices(), false);
                assert(ledgerTx.getInputs().size() == 2);
                assert(ledgerTx.getOutputs().size() == 2);

                IOUState outputIOU = ledgerTx.outputsOfType(IOUState.class).get(0);
                assert(outputIOU.equals(inputIOU.pay(Currencies.POUNDS(5))));

                // Sum all the output cash. This is complicated as there may be multiple cash output states with not all of them
                // being assigned to the lender.
                List<Cash.State> outputCash = ledgerTx.getOutputs().stream()
                        .map(state -> (Cash.State) state.getData())
                        .filter(state -> state.getOwner().getOwningKey().equals(b.getInfo().getLegalIdentities().get(0).getOwningKey()))
                        .collect(Collectors.toList());

                // Sum the acceptable cash sent to the lender
                Amount<Currency> outputCashSum = new Amount<>(0, inputIOU.amount.getToken());
                for (Cash.State cash: outputCash) {
                    Amount<Currency> addCash = new Amount<>(cash.getAmount().getQuantity(), cash.getAmount().getToken().getProduct());
                    outputCashSum = outputCashSum.plus(addCash);
                }

                assert (outputCashSum.equals(inputIOU.amount.minus(inputIOU.paid).minus(outputIOU.paid)));

                CommandWithParties command = ledgerTx.getCommands().get(0);
                assert (command.getValue().equals(new IOUContract.Commands.Settle()));

                settleResult.verifySignaturesExcept(b.getInfo().getLegalIdentities().get(0).getOwningKey(),
                        mockNetwork.getDefaultNotaryIdentity().getOwningKey());

                return null;
            } catch (Exception exception) {
                System.out.println(exception);
            }
            return null;
        });

    }

}