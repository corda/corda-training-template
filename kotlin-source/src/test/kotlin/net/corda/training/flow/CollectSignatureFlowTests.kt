package net.corda.training.flow

import net.corda.core.contracts.*
import net.corda.core.contracts.POUNDS
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.DUMMY_NOTARY
import net.corda.testing.node.MockNetwork
import net.corda.training.contract.IOUContract
import net.corda.core.flows.FlowLogic
import net.corda.training.state.IOUState
import net.corda.training.plugin.IOUPlugin
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Practical exercise instructions.
 * Uncomment the unit test and use the hints + unit test body to complete the FLows such that the unit tests pass.
 */
class CollectSignatureFlowTests {
    lateinit var net: MockNetwork
    lateinit var a: MockNetwork.MockNode
    lateinit var b: MockNetwork.MockNode

    @Before
    fun setup() {
        net = MockNetwork()
        val nodes = net.createSomeNodes(2)
        a = nodes.partyNodes[0]
        b = nodes.partyNodes[1]
        net.runNetwork()
    }

    @After
    fun tearDown() {
        net.stopNodes()
    }

    /**
     * Task 1.
     * We already have a flow that generates a partially signed transaction, now we need to build a flow that given
     * a [SignedTransaction] returns a [DigitalSignature] over that transaction. Instead of implementing this
     * functionality within the [IOUIssueFlow] we will use a generic set of two flows called [CollectSignatureFlow]
     * which can be used with multiple types of transaction, e.g. transferring and settling IOUs.
     * This [CollectSignatureFlow] can be called as a [subFlow] from the [IOUIssueFlow].
     * TODO: Implement the [IOUIssueFlow.Responder] flow which should return a [DigitalSignature.WithKey]
     * Hint:
     * - [CollectSignatureFlow.Initiator] is already implemented. It only has one statement which is a call of the
     *   [sendAndReceive] method which sends a [SignedTransaction] to the [otherParty] and waits for them to return a
     *   [DigitalSignature.WithKey].
     * - Implementing the responder requires a you to write a few lines of code...
     * - Most importantly, you'll need to [receive] the [SignedTransaction] from the [otherParty]. This method can be
     *   found here [FlowLogic.receive]
     * - Next, you'll need to [verify] the [SignedTransaction] and check the [otherParty]'s signature is valid.
     * - To [verify] a [SignedTransaction], you need to convert its [SignedTransaction.tx] property to a
     *   [LedgerTransaction].
     * - To check signatures use the [SignedTransaction.verifySignatures] method. You'll need to add the [CompositeKeys]
     *   for the signatures you DO NOT want to check, in this case, the party running the
     *   [CollectSignatureFlow.Responder] flow and the notary.
     * - Once you are satisfied the transaction is valid then you need to sign it with your [KeyPair] using the
     *   [SignedTransaction.signWithECDSA] method which returns a [DigitalSignature.WithKey].
     * - [send] the [DigitalSignature.WithKey] back to the [otherParty].
     */
    @Test
    fun checkCollectSignatureFlowResponderRespondsWithSignature() {
        val iou = IOUState(10.POUNDS, a.info.legalIdentity, b.info.legalIdentity)
        val issueCommand = Command(IOUContract.Commands.Issue(),
                listOf(a.info.legalIdentity.owningKey, b.info.legalIdentity.owningKey))
        val builder = TransactionType.General.Builder(DUMMY_NOTARY)
        builder.withItems(iou, issueCommand)
        val ptx = builder.signWith(a.services.legalIdentityKey).toSignedTransaction(false)
        val payload = CollectSignatureFlow.Payload(ptx, setOf(b.info.legalIdentity.owningKey))
        val future = a.services.startFlow(CollectSignatureFlow.Initiator(payload, b.info.legalIdentity)).resultFuture
        net.runNetwork()
        val signature = future.get()
        val stx = ptx + signature
        println(stx)
        println(stx.tx)
        println(stx.sigs)
        stx.verifySignatures(DUMMY_NOTARY.owningKey)
    }

    /**
     * Task 2.
     * Now the flow is complete, you need to register a flow initiator for the [CollectSignatureFlow].
     * If you forget to do the below then your CorDapp won't work!
     * TODO: Paste the following code inside the [CollectSignatureFlow] object.
     *
     *     class Service(services: PluginServiceHub) {
     *         init {
     *             services.registerFlowInitiator(CollectSignatureFlow.Initiator::class.java) {
     *                 CollectSignatureFlow.Responder(it)
     *             }
     *         }
     *     }
     *
     *  This code tells the Corda node running this CorDapp that when it receives a message concerning the
     *  [CollectSignatureFlow.Initiator] flow over the wire, it should respond by starting the
     *  [CollectSignatureFlow.Responder] flow.
     *
     *  Next: add the following entry to the [IOUPlugin.requiredFlows] map:
     *
     *      CollectSignatureFlow.Initiator::class.java.name to setOf(SignedTransaction::class.java.name,
     *                                                               Party::class.java.name)
     *
     *  This code whitelists the [CollectSignatureFlow.Initiator] flow, so that it can be invokved from a node.
     */
}

