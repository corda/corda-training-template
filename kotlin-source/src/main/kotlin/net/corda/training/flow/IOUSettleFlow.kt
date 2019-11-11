package net.corda.training.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.ProgressTracker
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.flows.CashIssueFlow
import net.corda.finance.workflows.asset.CashUtils
import net.corda.finance.workflows.getCashBalance
import net.corda.training.contract.IOUContract
import net.corda.training.state.IOUState
import java.util.*

/**
 * This is the flow which handles the (partial) settlement of existing IOUs on the ledger.
 * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
 * Notarisation (if required) and commitment to the ledger is handled vy the [FinalityFlow].
 * The flow returns the [SignedTransaction] that was committed to the ledger.
 */
@InitiatingFlow
@StartableByRPC
class IOUSettleFlow(val linearId: UniqueIdentifier, val amount: Amount<Currency>): FlowLogic<SignedTransaction>() {

    override val progressTracker = tracker()

    companion object {
        object CREATING : ProgressTracker.Step("Starting settlement of an IOU")
        object VERIFYING : ProgressTracker.Step("Verifying the IOU settlement")
        object SIGNING : ProgressTracker.Step("Signing the IOU settlement")
        object STORING : ProgressTracker.Step("Processing storage of IOU settlement") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(CREATING, SIGNING, VERIFYING, STORING)
    }

    @Suspendable
    override fun call(): SignedTransaction {
        progressTracker.currentStep = CREATING
        val notary = serviceHub.networkMapCache.notaryIdentities.single()

        val inputStateAndRef = getInputStateAndRef(linearId)
        val (utx, participants) = createUnsignedTransaction(notary, inputStateAndRef, amount)

        progressTracker.currentStep = VERIFYING
        utx.verify(serviceHub)

        progressTracker.currentStep = SIGNING
        val stx = serviceHub.signInitialTransaction(utx)

        val counterpartySessions = participants.filterNot { it == ourIdentity }.map { initiateFlow(it) }
        val ctx = subFlow(CollectSignaturesFlow(stx, counterpartySessions))

        return subFlow(FinalityFlow(ctx, counterpartySessions))
    }

    private fun getInputStateAndRef(linearId: UniqueIdentifier) : StateAndRef<IOUState> {
        val linearStateCriteria =
            QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        return serviceHub.vaultService.queryBy<IOUState>(linearStateCriteria).states.single()
    }

    @Suspendable
    private fun createUnsignedTransaction(
        notary: Party,
        inputStateAndRef: StateAndRef<IOUState>,
        amount: Amount<Currency>
    ): Pair<TransactionBuilder, List<Party>>{
        val inputIOU = inputStateAndRef.state.data

        checkTransaction(inputIOU, amount)

        val outputIOU = inputIOU.pay(amount)
        val participants = inputIOU.participants
        val command = Command(IOUContract.Commands.Settle(), participants.map { it.owningKey })
        val outputStateAndContract = StateAndContract(outputIOU, IOUContract.IOU_CONTRACT_ID)

        val baseBuilder = TransactionBuilder(notary = notary).withItems(
            inputStateAndRef,
            outputStateAndContract,
            command
        )

        val (builder, _) = CashUtils.generateSpend(
            serviceHub,
            baseBuilder,
            amount,
            ourIdentityAndCert,
            inputIOU.lender
        )
        return Pair(builder, participants)
    }

    private fun checkTransaction(inputIOU: IOUState, amount: Amount<Currency>) {
        val borrower = inputIOU.borrower
        if (borrower != ourIdentity) {
            throw IllegalArgumentException("Only the current borrower can initiate an IOU settlement")
        }

        val availableBalance = serviceHub.getCashBalance(inputIOU.amount.token)
        if (availableBalance.quantity <= 0) {
            throw IllegalArgumentException("Borrower does not have balance to settle in the relevant currency")
        }

        if (availableBalance < amount) {
            throw IllegalArgumentException("Borrower has only $availableBalance but needs $amount to settle.")
        }
    }
}

/**
 * This is the flow which signs IOU settlements.
 * The signing is handled by the [SignTransactionFlow].
 */
@InitiatedBy(IOUSettleFlow::class)
class IOUSettleFlowResponder(val flowSession: FlowSession): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val outputStates = stx.tx.outputs.map { it.data::class.java.name }.toList()
                "There must be an IOU transaction." using (outputStates.contains(IOUState::class.java.name))
            }
        }

        val signedTxID = subFlow(signedTransactionFlow).id
        subFlow(ReceiveFinalityFlow(flowSession, expectedTxId = signedTxID))
    }
}

@InitiatingFlow
@StartableByRPC
/**
 * Self issues the calling node an amount of cash in the desired currency.
 * Only used for demo/sample/training purposes!
 */
class SelfIssueCashFlow(val amount: Amount<Currency>) : FlowLogic<Cash.State>() {
    @Suspendable
    override fun call(): Cash.State {
        /** Create the cash issue command. */
        val issueRef = OpaqueBytes.of(0)
        /** Note: ongoing work to support multiple notary identities is still in progress. */
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        /** Create the cash issuance transaction. */
        val cashIssueTransaction = subFlow(CashIssueFlow(amount, issueRef, notary))
        /** Return the cash output. */
        return cashIssueTransaction.stx.tx.outputs.single().data as Cash.State
    }
}