package net.corda.training.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.crypto.CompositeKey
import net.corda.core.crypto.DigitalSignature
import net.corda.core.crypto.Party
import net.corda.core.crypto.signWithECDSA
import net.corda.core.flows.FlowLogic
import net.corda.core.node.PluginServiceHub
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.unwrap
import net.corda.flows.ResolveTransactionsFlow
import java.lang.IllegalStateException
import java.security.KeyPair

object SignTransactionFlow {
    class Service(services: PluginServiceHub) {
        init {
            services.registerFlowInitiator(SignTransactionFlow.Initiator::class.java) {
                SignTransactionFlow.Responder(it)
            }
        }
    }

    class Initiator(val wtx: WireTransaction): FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            verify(wtx)

            val allSigners = wtx.commands.flatMap { it.signers }.toSet()
            val myKey = serviceHub.myInfo.legalIdentity.owningKey
            val signatures = if (allSigners == setOf(myKey)) {
                listOf(sign(serviceHub.legalIdentityKey))
            } else {
                collectSignatures(allSigners)
            }

            val stx = SignedTransaction(wtx.serialized, signatures)
            if (needsNotarySignature(wtx)) {
                val notaryKey = wtx.notary?.owningKey
                stx.verifySignatures(notaryKey!!)
            } else {
                stx.verifySignatures()
            }

            return stx
        }

        @Suspendable
        private fun needsNotarySignature(wtx: WireTransaction) = wtx.inputs.isNotEmpty() || wtx.timestamp != null

        @Suspendable
        @Throws(IllegalArgumentException::class)
        private fun verify(proposal: WireTransaction) = proposal.toLedgerTransaction(serviceHub).verify()

        @Suspendable
        private fun sign(key: KeyPair) = key.signWithECDSA(wtx.id.bytes)

        @Suspendable
        private fun collectSignatures(keys: Set<CompositeKey>): List<DigitalSignature.WithKey> {
            return keys.map {
                val partyNode = serviceHub.networkMapCache.getNodeByLegalIdentityKey(it) ?:
                        throw IllegalStateException("Participant $it not found on the network.")
                partyNode.legalIdentity
            }.map {
                if (it == serviceHub.myInfo.legalIdentity) {
                    sign(serviceHub.legalIdentityKey)
                } else {
                    collectSignature(it, wtx)
                }
            }
        }

        @Suspendable
        private fun collectSignature(counterparty: Party, wtx: WireTransaction): DigitalSignature.WithKey {
            return sendAndReceive<DigitalSignature.WithKey>(counterparty, wtx).unwrap { it ->
                check(counterparty.owningKey.isFulfilledBy(it.by)) { "Not signed by the required participant." }
                it.verifyWithECDSA(wtx.id)
                it
            }
        }
    }

    class Responder(val otherParty: Party) : FlowLogic<Unit>() {
        @Suspendable
        override fun call(): Unit {
            val maybeProposal = receive<WireTransaction>(otherParty).unwrap { proposal ->
                checkMySignatureRequired(proposal)
                verifyDependencies(proposal)
                verify(proposal)
                proposal
            }
            val mySignature = sign(maybeProposal)
            send(otherParty, mySignature)
        }

        @Suspendable
        private fun sign(wtx: WireTransaction): DigitalSignature.WithKey {
            val myKey = serviceHub.legalIdentityKey
            return myKey.signWithECDSA(wtx.id.bytes)
        }

        @Suspendable
        @Throws(IllegalArgumentException::class)
        private fun verify(proposal: WireTransaction) = proposal.toLedgerTransaction(serviceHub).verify()

        @Suspendable
        private fun verifyDependencies(wtx: WireTransaction) = subFlow(ResolveTransactionsFlow(wtx, otherParty))

        @Suspendable
        private fun checkMySignatureRequired(wtx: WireTransaction) {
            // TODO: use keys from the keyManagementService instead.
            val myKey = serviceHub.myInfo.legalIdentity.owningKey
            require(myKey in wtx.mustSign) {
                "Party is not a participant for any of the input states of transaction ${wtx.id}"
            }
        }
    }
}