package net.corda.training.flow

import net.corda.core.contracts.*
import net.corda.core.crypto.DigitalSignature
import net.corda.core.crypto.Party
import net.corda.core.flows.FlowLogic
import net.corda.core.node.services.linearHeadsOfType
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.flows.FinalityFlow
import net.corda.training.contract.IOUContract
import net.corda.training.state.IOUState
import java.util.*

class IOUSettleFlow(val linearId: UniqueIdentifier, val amount: Amount<Currency>): FlowLogic<SignedTransaction>() {
    override fun call(): SignedTransaction {
        val me: Party = serviceHub.myInfo.legalIdentity
        // Retrieve the IOU state from the vault.
        val iouStates = serviceHub.vaultService.linearHeadsOfType<IOUState>()
        val iouToSettle = iouStates[linearId] ?: throw Exception("IOUState with linearId $linearId not found.")
        val counterparty: Party = iouToSettle.state.data.lender
        // Create a transaction builder.
        val notary = iouToSettle.state.notary
        val builder = TransactionType.General.Builder(notary)
        // Get some cash from the vault and add a spend to our transaction builder.
        serviceHub.vaultService.generateSpend(builder, amount, counterparty.owningKey)
        // Tx(Input cash, outputcash, cash pay command)
        // Add the IOU states and settle command to the transaction builder.
        val settleCommand: Command = Command(IOUContract.Commands.Settle(), listOf(counterparty.owningKey, me.owningKey))
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
        val wtx = builder.toWireTransaction()
        val stx = subFlow(SignTransactionFlow.Initiator(wtx))
        // Finalize the transaction.
        subFlow(FinalityFlow(stx, setOf(counterparty, me)))
        return stx
    }
}
