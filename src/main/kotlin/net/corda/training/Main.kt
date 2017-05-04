package net.corda.training

import com.google.common.util.concurrent.Futures
import net.corda.core.getOrThrow
import net.corda.core.node.services.ServiceInfo
import net.corda.node.driver.driver
import net.corda.node.services.transactions.ValidatingNotaryService
import net.corda.nodeapi.User

/**
 * This file is exclusively for being able to run your nodes through an IDE (as opposed to running deployNodes via
 * Gradle).
 *
 * Do not use in a production environment.
 *
 * To debug your CorDapp:
 *
 * 1. Run the "Run CorDapp - Kotlin" run configuration.
 * 2. Wait for all the nodes to start.
 * 3. Note the debug ports for each node, which should be output to the console. The "Debug CorDapp" configuration runs
 *    with port 5007, which should be "NodeA". In any case, double-check the console output to be sure.
 * 4. Set your breakpoints in your CorDapp code.
 * 5. Run the "Debug CorDapp" remote debug run configuration.
 */
fun main(args: Array<String>) {
    val user = User("user1", "test", permissions = setOf())
    driver(isDebug = true) {
        startNode("Controller", setOf(ServiceInfo(ValidatingNotaryService.Companion.type)))
        val (nodeA, nodeB, nodeC) = Futures.allAsList(
                startNode("NodeA", rpcUsers = listOf(user)),
                startNode("NodeB", rpcUsers = listOf(user)),
                startNode("NodeC", rpcUsers = listOf(user))).getOrThrow()
        startWebserver(nodeA)
        startWebserver(nodeB)
        startWebserver(nodeC)
        waitForAllNodesToFinish()
    }
}
