package net.corda.training.flow

import net.corda.core.contracts.*
import net.corda.core.crypto.CompositeKey
import net.corda.training.state.IOUState
import net.corda.training.contract.IOUContract
import net.corda.core.flows.FlowLogic
import net.corda.core.getOrThrow
import net.corda.core.utilities.DUMMY_NOTARY
import net.corda.testing.node.MockNetwork
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
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
        net.runNetwork()
    }

    @After
    fun tearDown() {
        net.stopNodes()
    }

    /**
     * Task 1.
     * Build out the [IOUIssueFlow]!
     * TODO: Implement the [IOUIssueFlow] flow which builds returns a [SignedTransaction].
     * Hint:
     * - There's a whole bunch of things you need to do to get this unit test to pass!
     * - Look at the comments in the [IOUIssueFlow] object for how to complete this task as well as the unit test below.
     * - Change the template type of the FlowLogic class and the return type of [call] (which is currently [Unit]) to
     *   [SignedTransaction].
     * - Create a [TransactionBuilder] and pass it a notary reference. You can get a reference to a transaction builder
     *   via [TransactionType.General.Builder]. A notary [Party] object can be obtained from
     *   [FlowLogic.serviceHub.networkMapService].
     * - Get a reference to your [KeyPair] as you'll need it to sign the transaction. It's available from
     *   [FlowLogic.serviceHub]. Remember that [IOUIssueFlow] is a sub-class of [FlowLogic].
     * - You can get a reference to a notary via [FlowLogic.serviceHub.networkMapCache].
     * - Create an IOU Issue [Command].
     * - Add the IOU state (as an output) and the [Command] to the transaction builder.
     * - Sign the transaction, you can use [TransactionBuilder.signWith].
     * - Convert the transaction builder to a [SignedTransaction] using the [toSignedTransation] method. Ensure that
     *   [checkSufficientSignatures] is set to false, otherwise Corda will assert all signatures should be present.
     * - Return the [SignedTransaction].
     */
    @Test
    fun flowReturnsCorrectlyFormedPartiallySignedTransaction() {
        val iou = IOUState(10.POUNDS, a.info.legalIdentity, b.info.legalIdentity, IOUContract())
        val flow = IOUIssueFlow(iou, b.info.legalIdentity)
        val future = a.services.startFlow(flow).resultFuture
        net.runNetwork()
        // Return the unsigned(!) SignedTransaction object from the IOUIssueFlow.
        val ptx: SignedTransaction = future.getOrThrow()
        // Print the transaction for debugging purposes.
        println(ptx.tx)
        // Check the transaction is well formed...
        // No outputs, one input IOUState and a command with the right properties.
        assert(ptx.tx.inputs.isEmpty())
        assert(ptx.tx.outputs.single().data is IOUState)
        val command = ptx.tx.commands.single()
        assert(command.value == IOUContract.Commands.Issue())
        assert(command.signers.toSet() == iou.participants.toSet())
        ptx.verifySignatures(b.info.legalIdentity.owningKey, DUMMY_NOTARY.owningKey)
    }

    /**
     * Task 2.
     * Now we have a well formed transaction, we need to properly verify it using the [IOUContract].
     * TODO: Amend the [IOUIssueFlow] to verify the transaction as well as sign it.
     * Hint:
     * - Remember: You can only verify a [LedgerTransaction].
     * - A [SignedTransaction] is a wrapper around a [WireTransaction] and a list of signatures.
     * - You can access the [WireTransaction] via [SignedTransaction.tx].
     * - Use [WireTransaction.toLedgerTransaction] method to get a [LedgerTransaction].
     */
    @Test
    fun flowReturnsVerifiedPartiallySignedTransaction() {
        // Check that a zero amount IOU fails.
        val zeroIou = IOUState(0.POUNDS, a.info.legalIdentity, b.info.legalIdentity, IOUContract())
        val futureOne = a.services.startFlow(IOUIssueFlow(zeroIou, b.info.legalIdentity)).resultFuture
        net.runNetwork()
        assertFailsWith<TransactionVerificationException> { futureOne.getOrThrow() }
        // Check that an IOU with the same participants fails.
        val borrowerIsLenderIou = IOUState(10.POUNDS, a.info.legalIdentity, a.info.legalIdentity, IOUContract())
        val futureTwo = a.services.startFlow(IOUIssueFlow(borrowerIsLenderIou, b.info.legalIdentity)).resultFuture
        net.runNetwork()
        assertFailsWith<TransactionVerificationException> { futureTwo.getOrThrow() }
        // Check a wrong state type fails.
        val wrongTypeState = object : ContractState {
            override val contract = DUMMY_PROGRAM_ID
            override val participants: List<CompositeKey> = listOf()
        }
        val futureThree = a.services.startFlow(IOUIssueFlow(wrongTypeState, b.info.legalIdentity)).resultFuture
        net.runNetwork()
        assertFailsWith<IllegalArgumentException> { futureThree.getOrThrow() }
        // Check a good IOU passes.
        val iou = IOUState(10.POUNDS, a.info.legalIdentity, b.info.legalIdentity, IOUContract())
        val futureFour = a.services.startFlow(IOUIssueFlow(iou, b.info.legalIdentity)).resultFuture
        net.runNetwork()
        futureFour.getOrThrow()
    }

    /**
     * Task 3.
     * Now we need to collect the signature from the [otherParty] using the [CollectSignatureFlow] which you have just
     * developed.
     * TODO: Amend the [IOUIssueFlow] to collect the [otherParty]'s signature.
     * Hint:
     * - You need to use [subFlow] to start the [CollectSignatureFlow.Initiator].
     * - Pass it the [SignedTransaction] object and the [Party] object for the [otherParty].
     * - When you receive the [DigitalSignature.WithKey] you'll need to add it to the [SignedTransaction]. You can use
     *   an overloaded '+' operator to do this.
     * - You'll then need to check the signature is valid. Use the same approach as you did in [CollectSignatureFlow]
     *   except now, you have all the signatures you need.
     * - Return the [SignedTransaction] with both signatures.
     */
    @Test
    fun flowReturnsTransactionSignedByBothParties() {
        val iou = IOUState(10.POUNDS, a.info.legalIdentity, b.info.legalIdentity, IOUContract())
        val flow = IOUIssueFlow(iou, b.info.legalIdentity)
        val future = a.services.startFlow(flow).resultFuture
        net.runNetwork()
        val stx = future.getOrThrow()
        stx.verifySignatures()
    }

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
    @Test
    fun flowRecordsTheSameTransactionInBothPartyVaults() {
        val iou = IOUState(10.POUNDS, a.info.legalIdentity, b.info.legalIdentity, IOUContract())
        val flow = IOUIssueFlow(iou, b.info.legalIdentity)
        val future = a.services.startFlow(flow).resultFuture
        net.runNetwork()
        val stx = future.getOrThrow()
        println("Signed transaction hash: ${stx.id}")
        listOf(a, b).map {
            it.storage.validatedTransactions.getTransaction(stx.id)
        }.forEach {
            val txHash = (it as SignedTransaction).id
            println("$txHash == ${stx.id}")
            assertEquals(stx.id, txHash)
        }
    }
}
