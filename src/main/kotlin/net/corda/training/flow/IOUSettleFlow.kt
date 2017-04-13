package net.corda.training.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.*
import net.corda.core.crypto.Party
import net.corda.core.flows.FlowLogic
import net.corda.core.node.services.linearHeadsOfType
import net.corda.core.transactions.SignedTransaction
import net.corda.flows.FinalityFlow
import net.corda.training.contract.IOUContract
import net.corda.training.state.IOUState
import java.util.*

class IOUSettleFlow(val linearId: UniqueIdentifier, val amount: Amount<Currency>): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val me: Party = serviceHub.myInfo.legalIdentity
        // Retrieve the IOU state from the vault.
        val iouStates = serviceHub.vaultService.linearHeadsOfType<IOUState>()
        val iouToSettle = iouStates[linearId] ?: throw Exception("IOUState with linearId $linearId not found.")
        val counterparty: Party = iouToSettle.state.data.lender
        if (me != iouToSettle.state.data.borrower) {
            throw IllegalArgumentException("IOU settlement flow must be initiated by the borrower.")
        }
        // Create a transaction builder.
        val notary = iouToSettle.state.notary
        val builder = TransactionType.General.Builder(notary)
        // Check we have enough cash.
        val cashBalance = serviceHub.vaultService.cashBalances[amount.token]
        if (cashBalance == null) {
            throw IllegalArgumentException("Borrower has no ${amount.token} to settle.")
        } else if(cashBalance < amount) {
            throw IllegalArgumentException("Borrower has only $cashBalance but needs $amount to settle.")
        }
        // Get some cash from the vault and add a spend to our transaction builder.
        serviceHub.vaultService.generateSpend(builder, amount, counterparty.owningKey)
        // Tx(Input cash, outputcash, cash pay command)
        // Add the IOU states and settle command to the transaction builder.
        val settleCommand = Command(IOUContract.Commands.Settle(), listOf(counterparty.owningKey, me.owningKey))
        // Add the input IOU and IOU settle command.
        builder.addCommand(settleCommand)
        builder.addInputState(iouToSettle)
        // Only add an output IOU state of the IOU has not been fully settled.
        val amountRemaining = iouToSettle.state.data.amount - iouToSettle.state.data.paid - amount
        if (amountRemaining > Amount(0, amount.token)) {
            val settledIOU: IOUState = iouToSettle.state.data.pay(amount)
            builder.addOutputState(settledIOU)
        }
        // Verify and sign the transaction.
        builder.toWireTransaction().toLedgerTransaction(serviceHub).verify()
        val ptx = builder.signWith(serviceHub.legalIdentityKey).toSignedTransaction(false)
        val stx = subFlow(SignTransactionFlow.Initiator(ptx))
        // Finalize the transaction.
        return subFlow(FinalityFlow(stx, setOf(counterparty, me))).single()
    }
}
