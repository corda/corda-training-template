package net.corda.training

import net.corda.core.internal.concurrent.transpose
import net.corda.core.utilities.getOrThrow
import net.corda.node.services.transactions.ValidatingNotaryService
import net.corda.nodeapi.User
import net.corda.nodeapi.internal.ServiceInfo
import net.corda.testing.ALICE
import net.corda.testing.BOB
import net.corda.testing.CHARLIE
import net.corda.testing.DUMMY_NOTARY
import net.corda.testing.driver.driver

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
    driver(isDebug = true, dsl = {
        val futures = listOf(
                startNode(providedName = DUMMY_NOTARY.name, advertisedServices = setOf(ServiceInfo(ValidatingNotaryService.type))),
                startNode(providedName = ALICE.name),
                startNode(providedName = BOB.name),
                startNode(providedName = CHARLIE.name))
        val nodes = futures.map { it.getOrThrow() }

        nodes.slice(IntRange(1, futures.size)).forEach { startWebserver(it) }

//        startWebserver(nodeA)
//        startWebserver(nodeB)
//        startWebserver(nodeC)

        waitForAllNodesToFinish()
    })
}