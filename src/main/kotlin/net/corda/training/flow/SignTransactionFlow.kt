package net.corda.training.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.crypto.CompositeKey
import net.corda.core.crypto.DigitalSignature
import net.corda.core.crypto.Party
import net.corda.core.crypto.signWithECDSA
import net.corda.core.flows.FlowLogic
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.unwrap
import net.corda.flows.ResolveTransactionsFlow
import java.lang.IllegalStateException

/**
 * The [SignTransactionFlow] automates the process of getting a transaction signed by all the required counterparties.
 */
object SignTransactionFlow {
    fun allSigners(wtx: WireTransaction) = wtx.commands.flatMap { it.signers }.toSet()
    /**
     * The [Initiator] collects all the signatures from the required parties defined within the [WireTransaction].
     */
    class Initiator(val ptx: SignedTransaction): FlowLogic<SignedTransaction>() {
        /**
         * 1. Verify the transaction.
         * 2. Get a list of all signers.
         * 3. Take off our key and check we have already signed the transaction.
         */
        @Suspendable
        override fun call(): SignedTransaction {
            // Reference to the encapsulated WireTransaction for ease.
            val wtx = ptx.tx
            // Verify the partially signed transaction.
            verify(wtx)
            // My key pair.
            val myKey = serviceHub.myInfo.legalIdentity.owningKey
            val notaryKey = serviceHub.networkMapCache.notaryNodes.single().notaryIdentity.owningKey
            // Get the list of counterparties which need to sign.
            val counterpartyCompositeKeys = allSigners(wtx) - myKey
            // Check that my signature has been added and is valid.
            ptx.verifySignatures(*counterpartyCompositeKeys.toTypedArray(), notaryKey)
            // Collect all the counterparty signatures.
            val counterpartySignatures = collectSignatures(counterpartyCompositeKeys)
            // Add the collected counterparty signatures to the list of signatures in the SignedTransaction.
            val stx = ptx + counterpartySignatures
            // Verify the collected signatures.
            if (needsNotarySignature(wtx)) {
                // Still need the notary signature.
                val notaryKey = wtx.notary?.owningKey
                stx.verifySignatures(notaryKey!!)
            } else {
                // We've collected all the signatures, e.g. for issuance transactions.
                stx.verifySignatures()
            }
            // Return the SignedTransaction with all but the notary signature (if required).
            return stx
        }

        /**
         * Check if this transaction needs a notary signature:
         * - It has input states
         * - It has a timestamp
         */
        @Suspendable
        private fun needsNotarySignature(wtx: WireTransaction) = wtx.inputs.isNotEmpty() || wtx.timestamp != null

        /**
         * Verify the transaction.
         */
        @Suspendable
        @Throws(IllegalArgumentException::class)
        private fun verify(proposal: WireTransaction) = proposal.toLedgerTransaction(serviceHub).verify()

        /**
         * Grab the [Party] objects for each [CompositeKey] and collect a signature in respect of it.
         */
        @Suspendable
        private fun collectSignatures(keys: Set<CompositeKey>): List<DigitalSignature.WithKey> {
            return keys.map {
                val partyNode = serviceHub.networkMapCache.getNodeByLegalIdentityKey(it) ?:
                        throw IllegalStateException("Participant $it not found on the network.")
                partyNode.legalIdentity
            }.map { collectSignature(it, ptx) }
        }

        /**
         * Collect a signature over the [WireTransaction] from the specified [Party].
         */
        @Suspendable
        private fun collectSignature(counterparty: Party, ptx: SignedTransaction): DigitalSignature.WithKey {
            // Collect and verify each signature when we get it.
            return sendAndReceive<DigitalSignature.WithKey>(counterparty, ptx).unwrap { it ->
                check(counterparty.owningKey.isFulfilledBy(it.by)) { "Not signed by the required participant." }
                it.verifyWithECDSA(ptx.tx.id)
                it
            }
        }
    }

    /**
     * The [Responder] receives a [WireTransaction], checks they need to sign, verifies the transaction, signs the
     * transaction and returns their [DigitalSignature] to the [Initiator].
     */
    abstract class AbstractResponder : FlowLogic<Unit>() {
        abstract val otherParty: Party
        /**
         * 1. Check the senders signature.
         * 2. Check the responding [Party] needs to sign the transaction.
         * 3. Resolve the dependencies required for this transaction.
         * 4. Verify this transaction.
         * 5. Perform any additional verification.
         * 6. Sign and send back the signature.
         */
        @Suspendable
        override fun call(): Unit {
            val checkedProposal = receive<SignedTransaction>(otherParty).unwrap { stx ->
                val proposal = stx.tx
                val notaryKey = serviceHub.networkMapCache.notaryNodes.single().notaryIdentity.owningKey
                val notCollectedYet = allSigners(proposal) - otherParty.owningKey
                stx.verifySignatures(*notCollectedYet.toTypedArray(), notaryKey)
                checkMySignatureRequired(proposal)
                verifyDependencies(proposal)
                verify(proposal)
                checkTransaction(proposal)
                proposal
            }
            val mySignature = sign(checkedProposal)
            send(otherParty, mySignature)
        }

        /**
         * Abstract method for performing additional checks depending on the expected transaction.
         */
        @Suspendable
        abstract fun checkTransaction(wtx: WireTransaction)

        /**
         * Sign the transaction with the node's [KeyPair].
         */
        @Suspendable
        private fun sign(wtx: WireTransaction): DigitalSignature.WithKey {
            val myKey = serviceHub.legalIdentityKey
            return myKey.signWithECDSA(wtx.id.bytes)
        }

        /**
         * Verify the transaction.
         */
        @Suspendable
        @Throws(IllegalArgumentException::class)
        private fun verify(proposal: WireTransaction) = proposal.toLedgerTransaction(serviceHub).verify()

        /**
         * Get all dependencies using the [ResolveTransactionsFlow].
         */
        @Suspendable
        private fun verifyDependencies(wtx: WireTransaction) = subFlow(ResolveTransactionsFlow(wtx, otherParty))

        /**
         * Check if our [CompositeKey] is in the list of required signers.
         */
        @Suspendable
        private fun checkMySignatureRequired(wtx: WireTransaction) {
            val myKey = serviceHub.myInfo.legalIdentity.owningKey
            require(myKey in wtx.mustSign) {
                "Party is not a participant for any of the input states of transaction ${wtx.id}"
            }
        }
    }
}