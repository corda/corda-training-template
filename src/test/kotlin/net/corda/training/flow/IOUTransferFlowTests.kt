package net.corda.training.flow

import net.corda.core.contracts.*
import net.corda.core.getOrThrow
import net.corda.core.transactions.SignedTransaction
import net.corda.testing.DUMMY_NOTARY
import net.corda.testing.node.MockNetwork
import net.corda.training.contract.IOUContract
import net.corda.training.state.IOUState
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
        // For real nodes this happens automatically, but we have to manually register the flow for tests
        nodes.partyNodes.forEach { it.registerInitiatedFlow(IOUIssueFlowResponder::class.java) }
        nodes.partyNodes.forEach { it.registerInitiatedFlow(IOUTransferFlowResponder::class.java) }
        net.runNetwork()
    }

    @After
    fun tearDown() {
        net.stopNodes()
    }

    /**
     * Issue an IOU on the ledger, we need to do this before we can transfer one.
     */
//    private fun issueIou(iou: IOUState): SignedTransaction {
//        val flow = IOUIssueFlow(iou)
//        val future = a.services.startFlow(flow).resultFuture
//        net.runNetwork()
//        return future.getOrThrow()
//    }

    /**
     * Task 1.
     * Build out the beginnings of [IOUTransferFlow]!
     * TODO: Implement the [IOUTransferFlow] flow which builds and returns a partially [SignedTransaction].
     * Hint:
     * - This flow will look similar to the [IOUIssueFlow].
     * - This time our transaction has an input state, so we need to retrieve it from the vault!
     * - You can use the [serviceHub.vaultQueryService.queryBy] method to get the latest linear states of a particular
     *   type from the vault. It returns a list of states matching your query.
     * - Use the [UniqueIdentifier] which is passed into the flow to retrieve the correct [IOUState].
     * - Use the [IOUState.withNewLender] method to create a copy of the state with a new lender.
     * - Create a Command - we will need to use the Transfer command.
     * - Remember, as we are involving three parties we will need to collect three signatures, so need to add three
     *   [PublicKey]s to the Command's signers list. We can get the signers from the input IOU and the new IOU you
     *   have just created with the new lender.
     * - Verify and sign the transaction as you did with the [IOUIssueFlow].
     * - Return the partially signed transaction.
     */
//    @Test
//    fun flowReturnsCorrectlyFormedPartiallySignedTransaction() {
//        val stx = issueIou(IOUState(10.POUNDS, a.info.legalIdentity, b.info.legalIdentity))
//        val inputIou = stx.tx.outputs.single().data as IOUState
//        val flow = IOUTransferFlow(inputIou.linearId, c.info.legalIdentity)
//        val future = a.services.startFlow(flow).resultFuture
//        net.runNetwork()
//        val ptx = future.getOrThrow()
//        // Check the transaction is well formed...
//        // One output IOUState, one input state reference and a Transfer command with the right properties.
//        assert(ptx.tx.inputs.size == 1)
//        assert(ptx.tx.outputs.size == 1)
//        assert(ptx.tx.inputs.single() == StateRef(stx.id, 0))
//        println("Input state ref: ${ptx.tx.inputs.single()} == ${StateRef(stx.id, 0)}")
//        val outputIou = ptx.tx.outputs.single().data as IOUState
//        println("Output state: $outputIou")
//        val command = ptx.tx.commands.single()
//        assert(command.value == IOUContract.Commands.Transfer())
//        ptx.verifySignaturesExcept(b.info.legalIdentity.owningKey, c.info.legalIdentity.owningKey, DUMMY_NOTARY.owningKey)
//    }

    /**
     * Task 2.
     * We need to make sure that only the current lender can execute this flow.
     * TODO: Amend the [IOUTransferFlow] to only allow the current lender to execute the flow.
     * Hint:
     * - Remember: You can use the node's identity and compare it to the [Party] object within the [IOUstate] you
     *   retrieved from the vault.
     * - Throw an [IllegalArgumentException] if the wrong party attempts to run the flow!
     */
//    @Test
//    fun flowCanOnlyBeRunByCurrentLender() {
//        val stx = issueIou(IOUState(10.POUNDS, a.info.legalIdentity, b.info.legalIdentity))
//        val inputIou = stx.tx.outputs.single().data as IOUState
//        val flow = IOUTransferFlow(inputIou.linearId, c.info.legalIdentity)
//        val future = b.services.startFlow(flow).resultFuture
//        net.runNetwork()
//        assertFailsWith<IllegalArgumentException> { future.getOrThrow() }
//    }

    /**
     * Task 3.
     * Check that an [IOUState] cannot be transferred to the same lender.
     * TODO: You shouldn't have to do anything additional to get this test to pass. Belts and Braces!
     */
//    @Test
//    fun iouCannotBeTransferredToSameParty() {
//        val stx = issueIou(IOUState(10.POUNDS, a.info.legalIdentity, b.info.legalIdentity))
//        val inputIou = stx.tx.outputs.single().data as IOUState
//        val flow = IOUTransferFlow(inputIou.linearId, a.info.legalIdentity)
//        val future = a.services.startFlow(flow).resultFuture
//        net.runNetwork()
//        // Check that we can't transfer an IOU to ourselves.
//        assertFailsWith<TransactionVerificationException> { future.getOrThrow() }
//    }

    /**
     * Task 4.
     * Get the borrowers and the new lenders signatures.
     * TODO: Amend the [SignTransactionFlow] to handle collecting signatures from multiple parties.
     * Hint: use the [SignTRansactionFlow] in the same way you did for the [IOUIssueFlow].
     */
//    @Test
//    fun flowReturnsTransactionSignedByAllParties() {
//        val stx = issueIou(IOUState(10.POUNDS, a.info.legalIdentity, b.info.legalIdentity))
//        val inputIou = stx.tx.outputs.single().data as IOUState
//        val flow = IOUTransferFlow(inputIou.linearId, c.info.legalIdentity)
//        val future = a.services.startFlow(flow).resultFuture
//        net.runNetwork()
//        future.getOrThrow().verifySignaturesExcept(DUMMY_NOTARY.owningKey)
//    }

    /**
     * Task 5.
     * Get the borrowers and the new lenders signatures.
     * TODO: Amend the [SignTransactionFlow] to handle collecting signatures from multiple parties.
     * Hint: use the [SignTRansactionFlow] in the same way you did for the [IOUIssueFlow].
     */
//    @Test
//    fun flowReturnsTransactionSignedByAllPartiesAndNotary() {
//        val stx = issueIou(IOUState(10.POUNDS, a.info.legalIdentity, b.info.legalIdentity))
//        val inputIou = stx.tx.outputs.single().data as IOUState
//        val flow = IOUTransferFlow(inputIou.linearId, c.info.legalIdentity)
//        val future = a.services.startFlow(flow).resultFuture
//        net.runNetwork()
//        future.getOrThrow().verifyRequiredSignatures()
//    }
}