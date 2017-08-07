package net.corda.training.flow

import net.corda.contracts.asset.Cash
import net.corda.contracts.asset.sumCash
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
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Practical exercise instructions.
 * Uncomment the unit tests and use the hints + unit test body to complete the FLows such that the unit tests pass.
 */
class IOUSettleFlowTests {
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
        nodes.partyNodes.forEach { it.registerInitiatedFlow(IOUSettleFlowResponder::class.java) }
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
     * Issue an some on-ledger cash to ourselves, we need to do this before we can Settle an IOU.
     */
    private fun issueCash(amount: Amount<Currency>): Cash.State {
        val flow = SelfIssueCashFlow(amount)
        val future = a.services.startFlow(flow).resultFuture
        net.runNetwork()
        return future.getOrThrow()
    }

    /**
     * Task 1.
     * The first task is to grab the [IOUState] for the given [linearId] from the vault, assemble a transaction
     * and sign it.
     * TODO: Grab the IOU for the given [linearId] from the vault, build and sign the settle transaction.
     * Hints:
     * - Use the code from the [IOUTransferFlow] to get the correct [IOUState] from the vault.
     * - You will need to use the [generateSpend] functionality of the vault to add the cash states and cash command
     *   to your transaction. The API is quite simple. It takes a reference to a [TransactionBuilder], an [Amount] and
     *   the [Party] object for the recipient. The function will mutate your builder by adding the states and commands.
     * - You then need to produce the output [IOUState] by using the [IOUState.pay] function.
     * - Add the input [IOUState] [StateAndRef] and the new output [IOUState] to the transaction.
     * - Sign the transaction and return it.
     */
//    @Test
//    fun flowReturnsCorrectlyFormedPartiallySignedTransaction() {
//        val stx = issueIou(IOUState(10.POUNDS, b.info.legalIdentity, a.info.legalIdentity))
//        issueCash(5.POUNDS)
//        val inputIou = stx.tx.outputs.single().data as IOUState
//        val flow = IOUSettleFlow(inputIou.linearId, 5.POUNDS)
//        val future = a.services.startFlow(flow).resultFuture
//        net.runNetwork()
//        val settleResult = future.getOrThrow()
//        // Check the transaction is well formed...
//        // One output IOUState, one input IOUState reference, input and output cash
//        val ledgerTx = settleResult.toLedgerTransaction(a.services)
//        assert(ledgerTx.inputs.size == 2)
//        assert(ledgerTx.outputs.size == 2)
//        val outputIou = ledgerTx.outputs.map { it.data }.filterIsInstance<IOUState>().single()
//        assertEquals(
//                outputIou,
//                inputIou.pay(5.POUNDS))
//        // Sum all the output cash. This is complicated as there may be multiple cash output states with not all of them
//        // being assigned to the lender.
//        val outputCashSum = ledgerTx.outputs
//                .map { it.data }
//                .filterIsInstance<Cash.State>()
//                .filter { it.owner == b.info.legalIdentity }
//                .sumCash()
//                .withoutIssuer()
//        // Compare the cash assigned to the lender with the amount claimed is being settled by the borrower.
//        assertEquals(
//                outputCashSum,
//                (inputIou.amount - inputIou.paid - outputIou.paid))
//        val command = ledgerTx.commands.requireSingleCommand<IOUContract.Commands>()
//        assert(command.value == IOUContract.Commands.Settle())
//        // Check the transaction has been signed by the borrower.
//        settleResult.verifySignaturesExcept(b.info.legalIdentity.owningKey, DUMMY_NOTARY.owningKey)
//    }

    /**
     * Task 2.
     * Only the borrower should be running this flow for a particular IOU.
     * TODO: Grab the IOU for the given [linearId] from the vault and check the node running the flow is the borrower.
     * Hint: Use the data within the iou obtained from the vault to check the right node is running the flow.
     */
//    @Test
//    fun settleFlowCanOnlyBeRunByBorrower() {
//        val stx = issueIou(IOUState(10.POUNDS, b.info.legalIdentity, a.info.legalIdentity))
//        issueCash(5.POUNDS)
//        val inputIou = stx.tx.outputs.single().data as IOUState
//        val flow = IOUSettleFlow(inputIou.linearId, 5.POUNDS)
//        val future = b.services.startFlow(flow).resultFuture
//        net.runNetwork()
//        assertFailsWith<IllegalArgumentException> { future.getOrThrow() }
//    }

    /**
     * Task 3.
     * The borrower must have at least SOME cash in the right currency to pay the lender.
     * TODO: Add a check in the flow to ensure that the borrower has a balance of cash in the right currency.
     * Hint:
     * - Use [serviceHub.vaultService.cashBalances] - it is a map which can be queried by [Currency].
     * - Use an if statement to check there is cash in the right currency present.
     */
//    @Test
//    fun borrowerMustHaveCashInRightCurrency() {
//        val stx = issueIou(IOUState(10.POUNDS, b.info.legalIdentity, a.info.legalIdentity))
//        val inputIou = stx.tx.outputs.single().data as IOUState
//        val flow = IOUSettleFlow(inputIou.linearId, 5.POUNDS)
//        val future = a.services.startFlow(flow).resultFuture
//        net.runNetwork()
//        assertFailsWith<IllegalArgumentException>("Borrower has no GBP to settle.") { future.getOrThrow() }
//    }

    /**
     * Task 4.
     * The borrower must have enough cash in the right currency to pay the lender.
     * TODO: Add a check in the flow to ensure that the borrower has enough cash to pay the lender.
     * Hint: Add another if statement similar to the one required above.
     */
//    @Test
//    fun borrowerMustHaveEnoughCashInRightCurrency() {
//        val stx = issueIou(IOUState(10.POUNDS, b.info.legalIdentity, a.info.legalIdentity))
//        issueCash(1.POUNDS)
//        val inputIou = stx.tx.outputs.single().data as IOUState
//        val flow = IOUSettleFlow(inputIou.linearId, 5.POUNDS)
//        val future = a.services.startFlow(flow).resultFuture
//        net.runNetwork()
//        assertFailsWith<IllegalArgumentException>("Borrower has only 1.00 GBP but needs 5.00 GBP to settle.") { future.getOrThrow() }
//    }

    /**
     * Task 5.
     * We need to get the transaction signed by the other party.
     * TODO: Use a subFlow call to the [SignTransactionFlow] to get a signature from the lender.
     */
//    @Test
//    fun flowReturnsTransactionSignedByBothParties() {
//        val stx = issueIou(IOUState(10.POUNDS, b.info.legalIdentity, a.info.legalIdentity))
//        issueCash(5.POUNDS)
//        val inputIou = stx.tx.outputs.single().data as IOUState
//        val flow = IOUSettleFlow(inputIou.linearId, 5.POUNDS)
//        val future = a.services.startFlow(flow).resultFuture
//        net.runNetwork()
//        val settleResult = future.getOrThrow()
//        // Check the transaction is well formed...
//        // One output IOUState, one input IOUState reference, input and output cash
//        settleResult.verifySignaturesExcept(DUMMY_NOTARY.owningKey)
//    }

    /**
     * Task 6.
     * We need to get the transaction signed by the notary service
     * TODO: Use a subFlow call to the [FinalityFlow] to get a signature from the lender.
     */
//    @Test
//    fun flowReturnsCommittedTransaction() {
//        val stx = issueIou(IOUState(10.POUNDS, b.info.legalIdentity, a.info.legalIdentity))
//        issueCash(5.POUNDS)
//        val inputIou = stx.tx.outputs.single().data as IOUState
//        val flow = IOUSettleFlow(inputIou.linearId, 5.POUNDS)
//        val future = a.services.startFlow(flow).resultFuture
//        net.runNetwork()
//        val settleResult = future.getOrThrow()
//        // Check the transaction is well formed...
//        // One output IOUState, one input IOUState reference, input and output cash
//        settleResult.verifyRequiredSignatures()
//    }
}
