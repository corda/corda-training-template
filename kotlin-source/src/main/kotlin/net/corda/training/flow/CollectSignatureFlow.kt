package net.corda.training.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.crypto.CompositeKey
import net.corda.core.crypto.DigitalSignature
import net.corda.core.crypto.Party
import net.corda.core.flows.FlowLogic
import net.corda.core.node.PluginServiceHub
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.unwrap
import net.corda.flows.ResolveTransactionsFlow

/**
 * These flows take a partially signed transaction and obtains a signature over the transaction provided by the
 * specified party.
 *
 * The [CollectSignatureFlow.Initiator] is started by the [Party] wanting to obtain a signature over a partially
 * signed transaction. The [Initiator] just sends the partially signed transaction to the specified [Party].
 *
 * The [CollectSignatureFlow.Responder] receives the transaction, checks the provided signatures, checks the
 * transaction validity (and resolves prior transactions if necessary), signs the transaction and sends back the
 * resultant signature.
 */
object CollectSignatureFlow {
    /**
     * This service registers a flow factory that is used when a initiating party attempts to communicate with us
     * using a particular flow. Registration is done against a marker class (in this case,
     * [CollectSignatureFlow.Initiator]) which is sent in the session handshake by the other party. If this marker
     * class has been registered then the corresponding factory will be used to create the flow which will communicate
     * with the other side. If there is no mapping, then the session attempt is rejected.
     *
     * In short, this bit of code is required for the recipient in this scenario to respond to the sender using the
     * [CollectSignatureFlow.Responder] flow.
     */
    class Service(services: PluginServiceHub) {
        init {
            services.registerFlowInitiator(CollectSignatureFlow.Initiator::class.java) {
                CollectSignatureFlow.Responder(it)
            }
        }
    }

    /**
     * Payload which encapsulates the partially signed transaction and the set of public keys for the signatures
     * we have already collected.
     */
    @CordaSerializable
    data class Payload(val ptx: SignedTransaction, val collected: Set<CompositeKey>)

    /**
     * Sends a partially signed transaction [ptx] to another party [otherParty] for signing. It expects to receive
     * a [DigitalSignature.WithKey] in response.
     */
    class Initiator(val payload: Payload, val otherParty: Party): FlowLogic<DigitalSignature.WithKey>() {
        @Suspendable
        override fun call(): DigitalSignature.WithKey {
            // Step 1. Send the partially signed transaction to the other party and receive a signature in return.
            return sendAndReceive<DigitalSignature.WithKey>(otherParty, payload).unwrap { it }
        }
    }

    /**
     * Receives a partially signed transaction [ptx] from another party [otherParty] for signing. It checks the
     * validity of the transaction and any existing signatures, signs and sends back a [DigitalSignature.WithKey]
     * in response.
     *
     * We don't need this flow to return anything.
     */
    class Responder(val otherParty: Party) : FlowLogic<Unit>() {
        @Suspendable
        override fun call(): Unit {
            // Step 2. Receive the partially signed transaction from the other party and perform validation over it.
            val ptx = receive<Payload>(otherParty).unwrap { (ptx, collected) ->
                // Step 3. Get a reference to the partially signed transaction and the notary.
                val notary = serviceHub.networkMapCache.notaryNodes.single().notaryIdentity
                // Step 4. Resolve any transaction dependencies if required.
                val dependencies = ptx.tx.inputs.map { it.txhash }.toSet()
                subFlow(ResolveTransactionsFlow(dependencies, otherParty))
                // Step 5. Verify the transaction.
                ptx.tx.toLedgerTransaction(serviceHub).verify()
                // Step 6. Verify the signature from the sending party.
                // Exclude public keys for signatures we haven't collected yet.
                // Get a set of all the signers.
                val allSigners = ptx.tx.commands.flatMap { it.signers }.toSet()
                // Get the notary's key.
                val notaryKey = notary.owningKey
                // Subtract the public keys we have already collected signatures in respect of and add the notary.
                val exclusions = allSigners.toSet().minus(collected) + notaryKey
                ptx.verifySignatures(*exclusions.toTypedArray())
                ptx
            }
            // Step 6. Add our signature and send it back to the other party.
            val myKeyPair = serviceHub.legalIdentityKey
            send(otherParty, ptx.signWithECDSA(myKeyPair))
        }
    }
}
