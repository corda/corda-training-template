package net.corda.hello.flow;

import net.corda.core.identity.CordaX500Name;
import net.corda.testing.node.*;
import java.util.*;
import net.corda.hello.MessageFlow;
import org.junit.*;
import org.junit.rules.ExpectedException;

public class SendMessageFlowTests {
    private MockNetwork mockNetwork;
    private StartedMockNode a, b;

    @Before
    public void setup() {
        MockNetworkParameters mockNetworkParameters = new MockNetworkParameters().withCordappsForAllNodes(
                Arrays.asList(TestCordapp.findCordapp("net.corda.hello"))).withNotarySpecs(Arrays.asList(new MockNetworkNotarySpec(
                new CordaX500Name("Notary", "London", "GB"))));
        mockNetwork = new MockNetwork(mockNetworkParameters);
        System.out.println(mockNetwork);

        a = mockNetwork.createNode(new MockNodeParameters());
        b = mockNetwork.createNode(new MockNodeParameters());

        ArrayList<StartedMockNode> startedNodes = new ArrayList<>();
        startedNodes.add(a);
        startedNodes.add(b);

        // For real nodes this happens automatically, but we have to manually register the flow for tests
        startedNodes.forEach(el -> el.registerInitiatedFlow(MessageFlow.Responder.class));
        mockNetwork.runNetwork();
    }

    @After
    public void tearDown() {
        mockNetwork.stopNodes();
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

}
