package net.corda.training.flow

import net.corda.core.contracts.Command
import net.corda.core.contracts.POUNDS
import net.corda.core.contracts.TransactionType
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.flows.FlowLogic
import net.corda.core.getOrThrow
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.testing.DUMMY_NOTARY
import net.corda.testing.node.MockNetwork
import net.corda.training.contract.IOUContract
import net.corda.training.state.IOUState
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Practical exercise instructions.
 * Uncomment the unit tests and use the hints + unit test body to complete the FLows such that the unit tests pass.
 */
class IOUIssueFlowTests {
    lateinit var net: MockNetwork
    lateinit var a: MockNetwork.MockNode
    lateinit var b: MockNetwork.MockNode

    @Before
    fun setup() {
        net = MockNetwork()
        val nodes = net.createSomeNodes(2)
        a = nodes.partyNodes[0]
        b = nodes.partyNodes[1]
        // For real nodes this happens automatically, but we have to manually register the flow for tests
        nodes.partyNodes.forEach { it.registerInitiatedFlow(IOUIssueFlowResponder::class.java) }
        net.runNetwork()
    }

    @After
    fun tearDown() {
        net.stopNodes()
    }

/**
     * Task 1.
     * Build out the [IOUIssueFlow]!
     * TODO: Implement the [IOUIssueFlow] flow which builds and returns a partially [SignedTransaction].
     * Hint:
     * - There's a whole bunch of things you need to do to get this unit test to pass!
     * - Look at the comments in the [IOUIssueFlow] object for how to complete this task as well as the unit test below.
     * - Change the template type of the FlowLogic class and the return type of [call] (which is currently [Unit]) to
     *   [SignedTransaction].
     * - Create a [TransactionBuilder] and pass it a notary reference. A notary [Party] object can be obtained from
     *   [FlowLogic.serviceHub.networkMapCache].
     * - Create an IOU Issue [Command].
     * - Add the IOU state (as an output) and the [Command] to the transaction builder.
     * - Sign the transaction and convert it to a [SignedTransaction] using the [ServiceHub.signInitialTransaction]
     *   method.
     * - Return the [SignedTransaction].
     */
//    @Test
//    fun flowReturnsCorrectlyFormedPartiallySignedTransaction() {
//        val iou = IOUState(10.POUNDS, a.info.legalIdentity, b.info.legalIdentity)
//        val flow = IOUIssueFlow(iou)
//        val future = a.services.startFlow(flow).resultFuture
//        net.runNetwork()
//        // Return the unsigned(!) SignedTransaction object from the IOUIssueFlow.
//        val ptx: SignedTransaction = future.getOrThrow()
//        // Print the transaction for debugging purposes.
//        println(ptx.tx)
//        // Check the transaction is well formed...
//        // No outputs, one input IOUState and a command with the right properties.
//        assert(ptx.tx.inputs.isEmpty())
//        assert(ptx.tx.outputs.single().data is IOUState)
//        val command = ptx.tx.commands.single()
//        assert(command.value == IOUContract.Commands.Issue())
//        assert(command.signers.toSet() == iou.participants.map { it.owningKey }.toSet())
//        ptx.verifySignaturesExcept(b.info.legalIdentity.owningKey, DUMMY_NOTARY.owningKey)
//    }

    /**
     * Task 2.
     * Now we have a well formed transaction, we need to properly verify it using the [IOUContract].
     * TODO: Amend the [IOUIssueFlow] to verify the transaction as well as sign it.
     */
//    @Test
//    fun flowReturnsVerifiedPartiallySignedTransaction() {
//        // Check that a zero amount IOU fails.
//        val zeroIou = IOUState(0.POUNDS, a.info.legalIdentity, b.info.legalIdentity)
//        val futureOne = a.services.startFlow(IOUIssueFlow(zeroIou)).resultFuture
//        net.runNetwork()
//        assertFailsWith<TransactionVerificationException> { futureOne.getOrThrow() }
//        // Check that an IOU with the same participants fails.
//        val borrowerIsLenderIou = IOUState(10.POUNDS, a.info.legalIdentity, a.info.legalIdentity)
//        val futureTwo = a.services.startFlow(IOUIssueFlow(borrowerIsLenderIou)).resultFuture
//        net.runNetwork()
//        assertFailsWith<TransactionVerificationException> { futureTwo.getOrThrow() }
//        // Check a good IOU passes.
//        val iou = IOUState(10.POUNDS, a.info.legalIdentity, b.info.legalIdentity)
//        val futureThree = a.services.startFlow(IOUIssueFlow(iou)).resultFuture
//        net.runNetwork()
//        futureThree.getOrThrow()
//    }

    /**
     * IMPORTANT: Review the [SignTransactionFlow] before continuing here.
     * Task 3.
     * Now we need to collect the signature from the [otherParty] using the [SignTransactionFlow] which has been
     * provided for you.
     * TODO: Amend the [IOUIssueFlow] to collect the [otherParty]'s signature.
     * Hint:
     * - You need to use [subFlow] to start the [SignTransactionFlow.Initiator].
     * - Pass it a [WireTransaction] object.
     * - It will return a [SignedTransaction].
     * - It performs the signature checking and transaction verification for you.
     * - Using this flow you abstract away all the back-and-forth communication required for parties to sign a
     *   transaction.
     */
//    @Test
//    fun flowReturnsTransactionSignedByBothParties() {
//        val iou = IOUState(10.POUNDS, a.info.legalIdentity, b.info.legalIdentity)
//        val flow = IOUIssueFlow(iou)
//        val future = a.services.startFlow(flow).resultFuture
//        net.runNetwork()
//        val stx = future.getOrThrow()
//        stx.verifyRequiredSignatures()
//    }

    /**
     * Now we need to store the finished [SignedTransaction] in both counter-party vaults.
     * TODO: Amend the [IOUIssueFlow] by adding a call to [FinalityFlow].
     * Hint:
     * - As mentioned above, use the [FinalityFlow] to ensure the transaction is recorded in both [Party] vaults.
     * - Do not use the [BroadcastTransactionFlow]!
     * - The [FinalityFlow] determines if the transaction requires notarisation or not.
     * - We don't need the notary's signature as this is an issuance transaction without a timestamp. There are no
     *   inputs in the transaction that could be double spent! If we added a timestamp to this transaction then we
     *   would require the notary's signature as notaries act as a timestamping authority.
     */
//    @Test
//    fun flowRecordsTheSameTransactionInBothPartyVaults() {
//        val iou = IOUState(10.POUNDS, a.info.legalIdentity, b.info.legalIdentity)
//        val flow = IOUIssueFlow(iou)
//        val future = a.services.startFlow(flow).resultFuture
//        net.runNetwork()
//        val stx = future.getOrThrow()
//        println("Signed transaction hash: ${stx.id}")
//        listOf(a, b).map {
//            it.services.validatedTransactions.getTransaction(stx.id)
//        }.forEach {
//            val txHash = (it as SignedTransaction).id
//            println("$txHash == ${stx.id}")
//            assertEquals(stx.id, txHash)
//        }
//    }
}
