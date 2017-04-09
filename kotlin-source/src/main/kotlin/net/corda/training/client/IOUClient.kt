package net.corda.training.client

import com.google.common.net.HostAndPort
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.loggerFor
import net.corda.client.rpc.CordaRPCClient
import net.corda.training.state.IOUState
import org.slf4j.Logger
import rx.Observable

/**
 * Demonstration of how to use the CordaRPCClient to connect to a Corda Node and
 * stream some State data from the node.
 */
fun main(args: Array<String>) {
    IOUClient().main(args)
}

private class IOUClient {
    companion object {
        val logger: Logger = loggerFor<IOUClient>()
    }

    fun main(args: Array<String>) {
        require(args.size == 1) { "Usage: IOUClient <node address>" }
        val nodeAddress = HostAndPort.fromString(args[0])
        val client = CordaRPCClient(nodeAddress)

        // Can be amended in the com.template.MainKt file.
        client.start("user1", "test")
        val proxy = client.proxy()

        // Grab all signed transactions and all future signed transactions.
        val (transactions: List<SignedTransaction>, futureTransactions: Observable<SignedTransaction>) =
                proxy.verifiedTransactions()

        // Log the existing TemplateStates and listen for new ones.
        futureTransactions.startWith(transactions).toBlocking().subscribe { transaction ->
            transaction.tx.outputs.forEach { output ->
                val state = output.data as IOUState
                logger.info(state.toString())
            }
        }
    }
}
