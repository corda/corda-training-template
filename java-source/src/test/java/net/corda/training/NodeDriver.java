package net.corda.training;

import net.corda.core.identity.CordaX500Name;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.driver.NodeParameters;
import net.corda.testing.node.NotarySpec;
import net.corda.testing.driver.NodeHandle;
import net.corda.testing.node.TestCordapp;
import net.corda.testing.node.User;
import net.corda.testing.driver.VerifierType;
import static net.corda.testing.driver.Driver.driver;
import net.corda.core.concurrent.CordaFuture;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList;
import java.util.*;

/**
 * This file is exclusively for being able to run your nodes through an IDE (as opposed to running deployNodes)
 * Do not use in a production environment.
 *
 * To debug your CorDapp:
 *
 * 1. Firstly, run the "Run Example CorDapp" run configuration.
 * 2. Wait for all the nodes to start.
 * 3. Note the debug ports which should be output to the console for each node. They typically start at 5006, 5007,
 *    5008. The "Debug CorDapp" configuration runs with port 5007, which should be "NodeB". In any case, double check
 *    the console output to be sure.
 * 4. Set your breakpoints in your CorDapp code.
 * 5. Run the "Debug CorDapp" remote debug run configuration.
 */
public class NodeDriver{
    public static void main(String[] args) {
        // No permissions required as we are not invoking flows.
        final User user = new User("user1", "test", ImmutableSet.of("ALL"));
        driver(new DriverParameters()
                .withIsDebug(true)
                .withWaitForAllNodesToFinish(true)
                .withCordappsForAllNodes(Arrays.asList(TestCordapp.findCordapp("net.corda.finance")))
                .withNotarySpecs(Arrays.asList(new NotarySpec(new CordaX500Name("Notary", "London","GB"), true,  Arrays.asList(user), VerifierType.InMemory, null))), dsl -> {
            CordaFuture<NodeHandle> partyAFuture = dsl.startNode(new NodeParameters()
                    .withProvidedName(new CordaX500Name("ParticipantA", "London", "GB"))
                    .withRpcUsers(ImmutableList.of(user)));
            CordaFuture<NodeHandle> partyBFuture = dsl.startNode(new NodeParameters()
                    .withProvidedName(new CordaX500Name("ParticipantB", "New York", "US"))
                    .withRpcUsers(ImmutableList.of(user)));
            CordaFuture<NodeHandle> partyCFuture = dsl.startNode(new NodeParameters()
                    .withProvidedName(new CordaX500Name("ParticipantC", "Paris", "FR"))
                    .withRpcUsers(ImmutableList.of(user)));

            try {
                dsl.startWebserver(partyAFuture.get());
                dsl.startWebserver(partyBFuture.get());
                dsl.startWebserver(partyCFuture.get());
            } catch (Throwable e) {
                System.err.println("Encountered exception in node startup: " + e.getMessage());
                e.printStackTrace();
            }

            return null;
        });
    }
}