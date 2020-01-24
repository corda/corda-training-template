package net.corda.hello;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static junit.framework.TestCase.assertTrue;

public class HelloTests {

    private MockNetwork mockNetwork;
    private StartedMockNode nodeA, nodeB;

    @Before
    public void setup() {
        MockNetworkParameters mockNetworkParameters = new MockNetworkParameters().withCordappsForAllNodes(Arrays.asList(TestCordapp.findCordapp("net.corda.hello")));
        mockNetwork = new MockNetwork(mockNetworkParameters);
        System.out.println(mockNetwork);
        nodeA = mockNetwork.createNode(new MockNodeParameters());
        nodeB = mockNetwork.createNode(new MockNodeParameters());
        ArrayList<StartedMockNode> startedNodes = new ArrayList<>();
        startedNodes.add(nodeA);
        startedNodes.add(nodeB);
        startedNodes.forEach(el -> el.registerInitiatedFlow(MessageFlow.Responder.class));
        mockNetwork.runNetwork();
    }

    @After
    public void tearDown() {
        mockNetwork.stopNodes();
    }

    /**
     * When you are ready to test your solution, comment out the BaselineTest and uncomment the SolutionTest.
     * Then you can run the SolutionTest from the run configurations or by clicking on the green arrow to the right of the test.
     **/

    @Test
    public void BaselineTest() throws Exception {
        Party partyA = nodeA.getInfo().getLegalIdentities().get(0);
        Party partyB = nodeB.getInfo().getLegalIdentities().get(0);

        CordaFuture<SignedTransaction> future = nodeA.startFlow(new MessageFlow.Initiator(partyB));
        mockNetwork.runNetwork();
        SignedTransaction tx = future.get();

        MessageState state = (MessageState) tx.getTx().getOutputStates().get(0);
        assert(state.origin.equals(partyA));
        assert(state.target.equals(partyB));
        assert(state.getParticipants().equals(ImmutableList.of(partyA, partyB)));

        List<StateAndRef<MessageState>> nodeAStates = nodeA.getServices().getVaultService().queryBy(MessageState.class).getStates();
        List<StateAndRef<MessageState>> nodeBStates = nodeB.getServices().getVaultService().queryBy(MessageState.class).getStates();
        assert(nodeAStates.size() == 1);
        assert(nodeBStates.size() == 1);

        MessageState nodeAState = nodeAStates.get(0).getState().getData();
        MessageState nodeBState = nodeBStates.get(0).getState().getData();
        assert(nodeAState.origin.equals(nodeBState.origin));
        assert(nodeAState.target.equals(nodeBState.target));
    }

//    @Test
//    public void SolutionTest() {
//        Party partyA = nodeA.getInfo().getLegalIdentities().get(0);
//        Party partyB = nodeB.getInfo().getLegalIdentities().get(0);
//
//        /* Test Task #1 */
//        MessageState state = new MessageState(partyA, partyB, "Hey!");
//        assertTrue("Task #1 Failed!", state.content.equals("Hey!"));
//
//        /* Test Task #2 */
//        nodeA.startFlow(new MessageFlow.Initiator(partyB, "Howdy!"));
//        mockNetwork.runNetwork();
//
//        /* Test Task #3 */
//        try {
//            CordaFuture future = nodeA.startFlow(new MessageFlow.Initiator(partyB, ""));
//            mockNetwork.runNetwork();
//            future.get();
//            assertTrue("Task #3 Failed!",false);
//        }
//        catch (Exception e) {
//            assertTrue("Task #3 Failed!", e.getCause().getCause() instanceof IllegalArgumentException);
//            assertTrue("Task #3 Failed!", e.getCause().getCause().getMessage().equals("Failed requirement: The content cannot be an empty String."));
//        }
//    }

}
