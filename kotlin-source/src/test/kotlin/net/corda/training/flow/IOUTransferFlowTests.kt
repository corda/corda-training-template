package net.corda.training.flow

import net.corda.core.contracts.*
import net.corda.core.utilities.DUMMY_NOTARY
import net.corda.training.state.IOUState
import net.corda.training.contract.IOUContract
import net.corda.core.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith

/**
 * Practical exercise instructions.
 * Uncomment the unit tests and use the hints + unit test body to complete the FLows such that the unit tests pass.
 */
class IOUTransferFlowTests {
    lateinit var net: MockNetwork
    lateinit var a: MockNetwork.MockNode
    lateinit var b: MockNetwork.MockNode
    lateinit var c: MockNetwork.MockNode

    @Before
    fun setup() {
        net = MockNetwork()
        val nodes = net.createSomeNodes(3)
        a = nodes.partyNodes[0]
        b = nodes.partyNodes[1]
        c = nodes.partyNodes[2]
        net.runNetwork()
    }

    @After
    fun tearDown() {
        net.stopNodes()
    }

    /**
     * Issue an IOU on the ledger, we need to do this before we can transfer one.
     */
    private fun issueIou(iou: IOUState): SignedTransaction {
        val flow = IOUIssueFlow(iou, b.info.legalIdentity)
        val future = a.services.startFlow(flow).resultFuture
        net.runNetwork()
        return future.getOrThrow()
    }

    /**
     * Task 1.
     * Build out the beginnings of [IOUTransferFlow]!
     * TODO: Implement the [IOUTransferFlow] flow which builds and returns a partially [SignedTransaction].
     * Hint:
     * - This flow will look similar to the [IOUIssueFlow].
     * - This time our transaction has an input state, so we need to retrieve it from the vault!
     * - You can use the [linearHeadsOfType] extention method to get the latest linear states of a particular type
     *   from the vault. It returns a [StateAndRef] object which contains the [IOUState].
     * - Use the [UniqueIdentifier] which is passed into the flow to retrieve the correct [IOUState].
     * - Use the [IOUState.withNewLender] method to create a copy of the state with a new lender.
     * - Create a Command - we will need to use the Transfer command.
     * - Remember, as we are involving three parties we will need to collect three signatures, so need to add three
     *   [CompositeKey]s to the Command's signers list. We can get the signers from the input IOU and the new IOU you
     *   have just created with the new lender.
     * - Verify and sign the transaction as you did with the [IOUIssueFlow].
     * - Return the partially signed transaction.
     */
    @Test
    fun flowReturnsCorrectlyFormedPartiallySignedTransaction() {
        val stx = issueIou(IOUState(10.POUNDS, a.info.legalIdentity, b.info.legalIdentity))
        val inputIou = stx.tx.outputs.single().data as IOUState
        val flow = IOUTransferFlow(inputIou.linearId, c.info.legalIdentity)
        val future = a.services.startFlow(flow).resultFuture
        net.runNetwork()
        val ptx = future.getOrThrow()
        // Check the transaction is well formed...
        // One output IOUState, one input state reference and a Transfer command with the right properties.
        assert(ptx.tx.inputs.size == 1)
        assert(ptx.tx.outputs.size == 1)
        assert(ptx.tx.inputs.single() == StateRef(stx.id, 0))
        println("Input state ref: ${ptx.tx.inputs.single()} == ${StateRef(stx.id, 0)}")
        val outputIou = ptx.tx.outputs.single().data as IOUState
        println("Output state: $outputIou")
        val command = ptx.tx.commands.single()
        assert(command.value == IOUContract.Commands.Transfer())
        ptx.verifySignatures(b.info.legalIdentity.owningKey, c.info.legalIdentity.owningKey, DUMMY_NOTARY.owningKey)
    }

    /**
     * Task 2.
     * We need to make sure that only the current lender can execute this flow.
     * TODO: Amend the [IOUTransferFlow] to only allow the current lender to execute the flow.
     * Hint:
     * - Remember: You can use the node's identity and compare it to the [Party] object within the [IOUstate] you
     *   retrieved from the vault.
     * - Throw an [IllegalArgumentException] if the wrong party attempts to run the flow!
     */
    @Test
    fun flowCanOnlyBeRunByCurrentLender() {
        val stx = issueIou(IOUState(10.POUNDS, a.info.legalIdentity, b.info.legalIdentity))
        val inputIou = stx.tx.outputs.single().data as IOUState
        val flow = IOUTransferFlow(inputIou.linearId, c.info.legalIdentity)
        val future = b.services.startFlow(flow).resultFuture
        net.runNetwork()
        assertFailsWith<IllegalArgumentException> { future.getOrThrow() }
    }

    /**
     * Task 3.
     * Check that an [IOUState] cannot be transferred to the same lender.
     * TODO: You shouldn't have to do anything additional to get this test to pass. Belts and Braces!
     */
    @Test
    fun iouCannotBeTransferredToSameParty() {
        val stx = issueIou(IOUState(10.POUNDS, a.info.legalIdentity, b.info.legalIdentity))
        val inputIou = stx.tx.outputs.single().data as IOUState
        val flow = IOUTransferFlow(inputIou.linearId, a.info.legalIdentity)
        val future = a.services.startFlow(flow).resultFuture
        net.runNetwork()
        // Check that we can't transfer an IOU to ourselves.
        assertFailsWith<TransactionVerificationException> { future.getOrThrow() }
    }

    /**
     * Task 4.
     * This is quite a tough one, so don't feel bad if you need to look at the solutions!
     * As the [IOUTransferFlow] flow requires signatures from two remote parties, we need to amend the
     * [CollectSignatureFlow] to handle collecting signatures from N parties.
     * TODO: Amend the [CollectSignatureFlow] to handle collecting signatures from multiple parties.
     * Hint:
     * - The main focus of our changes is keeping track of which signatures we have collected vs which ones we need to
     *   still collect.
     * - We can do this by passing in a set of [CompositeKey]s into the flow which represents the [CompositeKey]s for
     *   the signatures we have already collected.
     * - We can then compare the signers in the Transfer command to those in the set to see which ones we need to
     *   exclude when running [SignedTransaction.verifySignatures].
     * - We can only send one object via the flow framework, so we need to wrap up the [SignedTransaction] and the [Set]
     *   of [CompositeKeys] in a [Payload] object, we can use a "data" class to do this, e.g:
     *
     *       data class Payload(val stx: SignedTransaction, val collectedSigs: Set<CompositeKey)
     *
     *   This class needs to sit within the [CollectSignatureFlow].
     * - We now need to add an additional parameter to the constructor of [CollectSignatureFlow.Initiator] so that it
     *   takes a [Set] of [CompositeKey]s.
     * - We can then build the [Payload] object within the [Initiator] flow and send it to the responder. The
     *   [receive] method in the [Responder] flow will have to be updated to reflect the different type you are sending.
     * - Now you need to get a list of all signers in the transaction, compare it to the list of [CompositeKey]s for
     *   the signatrues you have already collected.
     * - You will also need to add the notary's public key as you will not have obtained that yet either.
     * - Pass the list of [CompositeKey]s not to check signatures for into the [SignedTransaction.verifySignatures]
     *   method.
     * - Now, when you call the [CollectSignatureFlow], you will need to pass it a [Set] of the [CompositeKey]s
     *   you have already obtained signatures for.
     * - Lastly... You'll need to change the parameter types for [CollectSignatureFlow.Initiator] in the
     *   [IOUPlugin.requiredFlows] property by adding the [Set] of [CompositeKey]s:
     *
     *      CollectSignatureFlow.Initiator::class.java.name to setOf(SignedTransaction::class.java.name,
     *                                                               Set::class.java.name,
     *                                                               Party::class.java.name)
     *
     * - Feel free to look at the solutions if you need to!
     */
    @Test
    fun flowReturnsTransactionSignedByAllParties() {
        val stx = issueIou(IOUState(10.POUNDS, a.info.legalIdentity, b.info.legalIdentity))
        val inputIou = stx.tx.outputs.single().data as IOUState
        val flow = IOUTransferFlow(inputIou.linearId, c.info.legalIdentity)
        val future = a.services.startFlow(flow).resultFuture
        net.runNetwork()
        // Check that we can't transfer an IOU to ourselves.
        future.getOrThrow().verifySignatures(DUMMY_NOTARY.owningKey)
    }
}
