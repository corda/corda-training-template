package net.corda.training

import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeParameters
import net.corda.testing.driver.driver
import net.corda.testing.node.User

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
fun main(args: Array<String>) {
    // No permissions required as we are not invoking flows.
    val user = User("user1", "test", permissions = setOf())
    driver(DriverParameters( isDebug = true,
            waitForAllNodesToFinish = true,
            extraCordappPackagesToScan = listOf("net.corda.finance"))) {
        startNode(NodeParameters(providedName = CordaX500Name("Controller", "London","GB"))).getOrThrow()
        val(nodeA, nodeB, nodeC) = listOf(
                startNode(NodeParameters(providedName = CordaX500Name("Bank A","London","GB")), rpcUsers = listOf(user)).getOrThrow(),
                startNode(NodeParameters(providedName = CordaX500Name("Bank B","New York","US")), rpcUsers = listOf(user)).getOrThrow(),
                startNode(NodeParameters(providedName = CordaX500Name("Bank C","Paris","FR")), rpcUsers = listOf(user)).getOrThrow())

        startWebserver(nodeA)
        startWebserver(nodeB)
        startWebserver(nodeC)
    }
}