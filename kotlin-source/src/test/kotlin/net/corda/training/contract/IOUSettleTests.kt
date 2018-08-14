package net.corda.training.contract

import net.corda.core.contracts.Amount
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.withoutIssuer
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.finance.DOLLARS
import net.corda.finance.POUNDS
import net.corda.finance.`issued by`
import net.corda.finance.contracts.asset.Cash
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import net.corda.training.ALICE
import net.corda.training.BOB
import net.corda.training.CHARLIE
import net.corda.training.state.IOUState
import org.junit.Test
import java.util.*

/**
 * Practical exercise instructions for Contracts Part 3.
 * The objective here is to write some contract code that verifies a transaction to settle an [IOUState].
 * Settling is more complicated than transfering and issuing as it requires you to use multiple state types in a
 * transaction.
 * As with the [IOUIssueTests] and [IOUTransferTests] uncomment each unit test and run them one at a time. Use the body
 * of the tests and the task description to determine how to get the tests to pass.
 */
class IOUSettleTests {
    private fun createCashState(amount: Amount<Currency>, owner: AbstractParty): Cash.State {
        val defaultRef = ByteArray(1, { 1 })
        return Cash.State(amount = amount `issued by`
                TestIdentity(CordaX500Name(organisation = "MegaCorp", locality = "MegaPlanet", country = "US")).ref(defaultRef.first()),
                owner = owner)
    }

    // A pre-defined dummy command.
    class DummyCommand : TypeOnlyCommandData()
    var ledgerServices = MockServices(listOf("net.corda.training"))

    /**
     * Task 1.
     * We need to add another case to deal with settling in the [IOUContract.verify] function.
     * TODO: Add the [IOUContract.Commands.Settle] case to the verify function.
     * Hint: You can leave the body empty for now.
     */
//    @Test
//    fun mustIncludeSettleCommand() {
//        val iou = IOUState(10.POUNDS, ALICE.party, BOB.party)
//        val inputCash = createCashState(5.POUNDS, BOB.party)
//        val outputCash = inputCash.withNewOwner(newOwner = ALICE.party).ownableState
//        ledgerServices.ledger {
//            transaction {
//                input(IOUContract.IOU_CONTRACT_ID, iou)
//                output(IOUContract.IOU_CONTRACT_ID, iou.pay(5.POUNDS))
//                input(IOUContract.IOU_CONTRACT_ID, inputCash)
//                output(IOUContract.IOU_CONTRACT_ID, outputCash)
//                command(BOB.publicKey, Cash.Commands.Move())
//                this.fails()
//            }
//            transaction {
//                input(IOUContract.IOU_CONTRACT_ID, iou)
//                output(IOUContract.IOU_CONTRACT_ID, iou.pay(5.POUNDS))
//                input(IOUContract.IOU_CONTRACT_ID, inputCash)
//                output(IOUContract.IOU_CONTRACT_ID, outputCash)
//                command(BOB.publicKey, Cash.Commands.Move())
//                command(listOf(ALICE.publicKey, BOB.publicKey), DummyCommand()) // Wrong type.
//                this.fails()
//            }
//            transaction {
//                input(IOUContract.IOU_CONTRACT_ID, iou)
//                output(IOUContract.IOU_CONTRACT_ID, iou.pay(5.POUNDS))
//                input(IOUContract.IOU_CONTRACT_ID, inputCash)
//                output(IOUContract.IOU_CONTRACT_ID, outputCash)
//                command(BOB.publicKey, Cash.Commands.Move())
//                command(listOf(ALICE.publicKey, BOB.publicKey), IOUContract.Commands.Settle()) // Correct Type.
//                this.verifies()
//            }
//        }
//    }

    /**
     * Task 2.
     * For now, we only want to settle one IOU at once. We can use the [TransactionForContract.groupStates] function
     * to group the IOUs by their [lienarId] property. We want to make sure there is only one group of input and output
     * IOUs.
     * TODO: Using [groupStates] add a constraint that checks for one group of input/output IOUs.
     * Hint:
     * - The [single] function enforces a single element in a list or throws an exception.
     * - The [groupStates] function takes two type parameters: the type of the state you wish to group by and the type
     *   of the grouping key used, in this case as you need to use the [linearId] and it is a [UniqueIdentifier].
     * - The [groupStates] also takes a lambda function which selects a property of the state to decide the groups.
     * - In Kotlin if the last argument of a function is a lambda, you can call it like this:
     *
     *       fun functionWithLambda() { it.property }
     *
     *   This is exactly how map / filter are used in Kotlin.
     */
//    @Test
//    fun mustBeOneGroupOfIOUs() {
//        val iouOne = IOUState(10.POUNDS, ALICE.party, BOB.party)
//        val iouTwo = IOUState(5.POUNDS, ALICE.party, BOB.party)
//        val inputCash = createCashState(5.POUNDS, BOB.party)
//        val outputCash = inputCash.withNewOwner(newOwner = ALICE.party)
//        ledgerServices.ledger {
//            transaction {
//                input(IOUContract.IOU_CONTRACT_ID, iouOne)
//                input(IOUContract.IOU_CONTRACT_ID, iouTwo)
//                command(listOf(ALICE.publicKey, BOB.publicKey), IOUContract.Commands.Settle())
//                output(IOUContract.IOU_CONTRACT_ID, iouOne.pay(5.POUNDS))
//                input(IOUContract.IOU_CONTRACT_ID, inputCash)
//                output(IOUContract.IOU_CONTRACT_ID, outputCash.ownableState)
//                command(BOB.publicKey, Cash.Commands.Move())
//                this `fails with` "List has more than one element."
//            }
//            transaction {
//                input(IOUContract.IOU_CONTRACT_ID, iouOne)
//                command(listOf(ALICE.publicKey, BOB.publicKey), IOUContract.Commands.Settle())
//                output(IOUContract.IOU_CONTRACT_ID, iouOne.pay(5.POUNDS))
//                input(IOUContract.IOU_CONTRACT_ID, inputCash)
//                output(IOUContract.IOU_CONTRACT_ID, outputCash.ownableState)
//                command(BOB.publicKey, Cash.Commands.Move())
//                this.verifies()
//            }
//        }
//    }

    /**
     * Task 3.
     * There always has to be one input IOU in a settle transaction but there might not be an output IOU.
     * TODO: Add a constraint to check there is always one input IOU.
     */
//    @Test
//    fun mustHaveOneInputIOU() {
//        val iou = IOUState(10.POUNDS, ALICE.party, BOB.party)
//        val iouOne = IOUState(10.POUNDS, ALICE.party, BOB.party)
//        val tenPounds = createCashState(10.POUNDS, BOB.party)
//        val fivePounds = createCashState(5.POUNDS, BOB.party)
//        ledgerServices.ledger {
//            transaction {
//                command(listOf(ALICE.publicKey, BOB.publicKey), IOUContract.Commands.Settle())
//                output(IOUContract.IOU_CONTRACT_ID, iou)
//                this `fails with` "There must be one input IOU."
//            }
//            transaction {
//                input(IOUContract.IOU_CONTRACT_ID, iouOne)
//                command(listOf(ALICE.publicKey, BOB.publicKey), IOUContract.Commands.Settle())
//                output(IOUContract.IOU_CONTRACT_ID, iouOne.pay(5.POUNDS))
//                input(IOUContract.IOU_CONTRACT_ID, fivePounds)
//                output(IOUContract.IOU_CONTRACT_ID, fivePounds.withNewOwner(newOwner = ALICE.party).ownableState)
//                command(BOB.publicKey, Cash.Commands.Move())
//                this.verifies()
//            }
//            transaction {
//                input(IOUContract.IOU_CONTRACT_ID, iouOne)
//                command(listOf(ALICE.publicKey, BOB.publicKey), IOUContract.Commands.Settle())
//                input(IOUContract.IOU_CONTRACT_ID, tenPounds)
//                output(IOUContract.IOU_CONTRACT_ID, tenPounds.withNewOwner(newOwner = ALICE.party).ownableState)
//                command(BOB.publicKey, Cash.Commands.Move())
//                this.verifies()
//            }
//        }
//    }

    /**
     * Task 4.
     * Now we need to ensure that there are cash states present in the outputs list. The [IOUContract] doesn't care
     * about input cash as the validity of the cash transaction will be checked by the [Cash] contract. We do however
     * need to count how much cash is being used to settle and update our [IOUState] accordingly.
     * TODO: Filter out the cash states from the list of outputs list and assign them to a constant.
     * Hint:
     * - Use the [outputsOfType] extension function to filter the transaction's outputs by type, in this case [Cash.State].
     */
//    @Test
//    fun mustBeCashOutputStatesPresent() {
//        val iou = IOUState(10.DOLLARS, ALICE.party, BOB.party)
//        val cash = createCashState(5.DOLLARS, BOB.party)
//        val cashPayment = cash.withNewOwner(newOwner = ALICE.party)
//        ledgerServices.ledger {
//            transaction {
//                input(IOUContract.IOU_CONTRACT_ID, iou)
//                output(IOUContract.IOU_CONTRACT_ID, iou.pay(5.DOLLARS))
//                command(listOf(ALICE.publicKey, BOB.publicKey), IOUContract.Commands.Settle())
//                this `fails with` "There must be output cash."
//            }
//            transaction {
//                input(IOUContract.IOU_CONTRACT_ID, iou)
//                input(IOUContract.IOU_CONTRACT_ID, cash)
//                output(IOUContract.IOU_CONTRACT_ID, iou.pay(5.DOLLARS))
//                output(IOUContract.IOU_CONTRACT_ID, cashPayment.ownableState)
//                command(BOB.publicKey, cashPayment.command)
//                command(listOf(ALICE.publicKey, BOB.publicKey), IOUContract.Commands.Settle())
//                this.verifies()
//            }
//        }
//    }

    /**
     * Task 5.
     * Not only to we need to check that [Cash] output states are present but we need to check that the payer is
     * correctly assigning us as the new owner of these states.
     * TODO: Add a constaint to check that we are the new owner of the output cash.
     * Hint:
     * - Not all of the cash may be assigned to us as some of the input cash may be sent back to the payer as change.
     * - We need to use the [Cash.State.owner] property to check to see that it is the value of our public key.
     * - Use [filter] to filter over the list of cash states to get the ones which are being assigned to us.
     * - Once we have this filtered list, we can sum the cash being paid to us so we know how much is being settled.
     */
//    @Test
//    fun mustBeCashOutputStatesWithRecipientAsOwner() {
//        val iou = IOUState(10.POUNDS, ALICE.party, BOB.party)
//        val cash = createCashState(5.POUNDS, BOB.party)
//        val invalidCashPayment = cash.withNewOwner(newOwner = CHARLIE.party)
//        val validCashPayment = cash.withNewOwner(newOwner = ALICE.party)
//        ledgerServices.ledger {
//            transaction {
//                input(IOUContract.IOU_CONTRACT_ID, iou)
//                input(IOUContract.IOU_CONTRACT_ID, cash)
//                output(IOUContract.IOU_CONTRACT_ID, iou.pay(5.POUNDS))
//                output(IOUContract.IOU_CONTRACT_ID, "outputs cash", invalidCashPayment.ownableState)
//                command(BOB.publicKey, invalidCashPayment.command)
//                command(listOf(ALICE.publicKey, BOB.publicKey), IOUContract.Commands.Settle())
//                this `fails with` "There must be output cash paid to the recipient."
//            }
//            transaction {
//                input(IOUContract.IOU_CONTRACT_ID, iou)
//                input(IOUContract.IOU_CONTRACT_ID, cash)
//                output(IOUContract.IOU_CONTRACT_ID, iou.pay(5.POUNDS))
//                output(IOUContract.IOU_CONTRACT_ID, validCashPayment.ownableState)
//                command(BOB.publicKey, validCashPayment.command)
//                command(listOf(ALICE.publicKey, BOB.publicKey), IOUContract.Commands.Settle())
//                this.verifies()
//            }
//        }
//    }

    /**
     * Task 6.
     * Now we need to sum the cash which is being assigned to us and compare this total against how much of the iou is
     * left to pay.
     * TODO: Add a constraint that checks we cannot be paid more than the remaining IOU amount left to pay.
     * Hint:
     * - The remaining amount of the IOU is the amount less the paid property.
     * - To sum a list of [Cash.State]s use the [sumCash] function.
     * - The [sumCash] function returns an [Issued<Amount<Currency>>] type. We don't care about the issuer so we can
     *   apply [withoutIssuer] to unwrap the [Amount] from [Issuer].
     * - We can compare the amount left paid to the amount being paid to use, ensuring the amount being paid isn't too
     *   much.
     */
//    @Test
//    fun cashSettlementAmountMustBeLessThanRemainingIOUAmount() {
//        val iou = IOUState(10.DOLLARS, ALICE.party, BOB.party)
//        val elevenDollars = createCashState(11.DOLLARS, BOB.party)
//        val tenDollars = createCashState(10.DOLLARS, BOB.party)
//        val fiveDollars = createCashState(5.DOLLARS, BOB.party)
//        ledgerServices.ledger {
//            transaction {
//                input(IOUContract.IOU_CONTRACT_ID, iou)
//                input(IOUContract.IOU_CONTRACT_ID, elevenDollars)
//                output(IOUContract.IOU_CONTRACT_ID, iou.pay(11.DOLLARS))
//                output(IOUContract.IOU_CONTRACT_ID, elevenDollars.withNewOwner(newOwner = ALICE.party).ownableState)
//                command(BOB.publicKey, elevenDollars.withNewOwner(newOwner = ALICE.party).command)
//                command(listOf(ALICE.publicKey, BOB.publicKey), IOUContract.Commands.Settle())
//                this `fails with` "The amount settled cannot be more than the amount outstanding."
//            }
//            transaction {
//                input(IOUContract.IOU_CONTRACT_ID, iou)
//                input(IOUContract.IOU_CONTRACT_ID, fiveDollars)
//                output(IOUContract.IOU_CONTRACT_ID, iou.pay(5.DOLLARS))
//                output(IOUContract.IOU_CONTRACT_ID, fiveDollars.withNewOwner(newOwner = ALICE.party).ownableState)
//                command(BOB.publicKey, fiveDollars.withNewOwner(newOwner = ALICE.party).command)
//                command(listOf(ALICE.publicKey, BOB.publicKey), IOUContract.Commands.Settle())
//                this.verifies()
//            }
//            transaction {
//                input(IOUContract.IOU_CONTRACT_ID, iou)
//                input(IOUContract.IOU_CONTRACT_ID, tenDollars)
//                output(IOUContract.IOU_CONTRACT_ID, tenDollars.withNewOwner(newOwner = ALICE.party).ownableState)
//                command(BOB.publicKey, tenDollars.withNewOwner(newOwner = ALICE.party).command)
//                command(listOf(ALICE.publicKey, BOB.publicKey), IOUContract.Commands.Settle())
//                this.verifies()
//            }
//        }
//    }

    /**
     * Task 7.
     * Kotlin's type system should handle this for you but it goes without saying that we should only be able to settle
     * in the currency that the IOU in denominated in.
     * TODO: You shouldn't have anything to do here but here are some tests just to make sure!
     */
//    @Test
//    fun cashSettlementMustBeInTheCorrectCurrency() {
//        val iou = IOUState(10.DOLLARS, ALICE.party, BOB.party)
//        val tenDollars = createCashState(10.DOLLARS, BOB.party)
//        val tenPounds = createCashState(10.POUNDS, BOB.party)
//        ledgerServices.ledger {
//            transaction {
//                input(IOUContract.IOU_CONTRACT_ID, iou)
//                input(IOUContract.IOU_CONTRACT_ID, tenPounds)
//                output(IOUContract.IOU_CONTRACT_ID, tenPounds.withNewOwner(newOwner = ALICE.party).ownableState)
//                command(BOB.publicKey, tenPounds.withNewOwner(newOwner = ALICE.party).command)
//                command(listOf(ALICE.publicKey, BOB.publicKey), IOUContract.Commands.Settle())
//                this `fails with` "Token mismatch: GBP vs USD"
//            }
//            transaction {
//                input(IOUContract.IOU_CONTRACT_ID, iou)
//                input(IOUContract.IOU_CONTRACT_ID, tenDollars)
//                output(IOUContract.IOU_CONTRACT_ID, tenDollars.withNewOwner(newOwner = ALICE.party).ownableState)
//                command(BOB.publicKey, tenDollars.withNewOwner(newOwner = ALICE.party).command)
//                command(listOf(ALICE.publicKey, BOB.publicKey), IOUContract.Commands.Settle())
//                this.verifies()
//            }
//        }
//    }

    /**
     * Task 8.
     * If we fully settle the IOU, then we are done and thus don't require one on ledgerServices.ledger anymore. However, if we only
     * partially settle the IOU, then we want to keep the IOU on ledger with an amended [paid] property.
     * TODO: Write a constraint that ensures the correct behaviour depending on the amount settled vs amount remaining.
     * Hint: You can use a simple if statement and compare the total amount paid vs amount left to settle.
     */
//    @Test
//    fun mustOnlyHaveOutputIOUIfNotFullySettling() {
//        val iou = IOUState(10.DOLLARS, ALICE.party, BOB.party)
//        val tenDollars = createCashState(10.DOLLARS, BOB.party)
//        val fiveDollars = createCashState(5.DOLLARS, BOB.party)
//        ledgerServices.ledger {
//            transaction {
//                input(IOUContract.IOU_CONTRACT_ID, iou)
//                input(IOUContract.IOU_CONTRACT_ID, fiveDollars)
//                output(IOUContract.IOU_CONTRACT_ID, fiveDollars.withNewOwner(newOwner = ALICE.party).ownableState)
//                command(BOB.publicKey, fiveDollars.withNewOwner(newOwner = BOB.party).command)
//                command(listOf(ALICE.publicKey, BOB.publicKey), IOUContract.Commands.Settle())
//                this `fails with` "There must be one output IOU."
//            }
//            transaction {
//                input(IOUContract.IOU_CONTRACT_ID, iou)
//                input(IOUContract.IOU_CONTRACT_ID, fiveDollars)
//                output(IOUContract.IOU_CONTRACT_ID, fiveDollars.withNewOwner(newOwner = ALICE.party).ownableState)
//                output(IOUContract.IOU_CONTRACT_ID, iou.pay(5.DOLLARS))
//                command(BOB.publicKey, fiveDollars.withNewOwner(newOwner = BOB.party).command)
//                command(listOf(ALICE.publicKey, BOB.publicKey), IOUContract.Commands.Settle())
//                verifies()
//            }
//            transaction {
//                input(IOUContract.IOU_CONTRACT_ID, tenDollars)
//                input(IOUContract.IOU_CONTRACT_ID, iou)
//                output(IOUContract.IOU_CONTRACT_ID, iou.pay(10.DOLLARS))
//                output(IOUContract.IOU_CONTRACT_ID, tenDollars.withNewOwner(newOwner = ALICE.party).ownableState)
//                command(BOB.publicKey, tenDollars.withNewOwner(newOwner = BOB.party).command)
//                command(listOf(ALICE.publicKey, BOB.publicKey), IOUContract.Commands.Settle())
//                this `fails with` "There must be no output IOU as it has been fully settled."
//            }
//            transaction {
//                input(IOUContract.IOU_CONTRACT_ID, tenDollars)
//                input(IOUContract.IOU_CONTRACT_ID, iou)
//                output(IOUContract.IOU_CONTRACT_ID, tenDollars.withNewOwner(newOwner = ALICE.party).ownableState)
//                command(BOB.publicKey, tenDollars.withNewOwner(newOwner = BOB.party).command)
//                command(listOf(ALICE.publicKey, BOB.publicKey), IOUContract.Commands.Settle())
//                verifies()
//            }
//        }
//    }

    /**
     * Task 9.
     * We want to make sure that the only property of the IOU which changes when we settle, is the paid amount.
     * TODO: Write a constraint to check only the paid property of the [IOUState] changes when settling.
     */
//    @Test
//    fun onlyPaidPropertyMayChange() {
//        val iou = IOUState(10.DOLLARS, ALICE.party, BOB.party)
//        val fiveDollars = createCashState(5.DOLLARS, BOB.party)
//        ledgerServices.ledger {
//            transaction {
//                input(IOUContract.IOU_CONTRACT_ID, iou)
//                input(IOUContract.IOU_CONTRACT_ID, fiveDollars)
//                output(IOUContract.IOU_CONTRACT_ID, fiveDollars.withNewOwner(newOwner = ALICE.party).ownableState)
//                output(IOUContract.IOU_CONTRACT_ID, iou.copy(borrower = ALICE.party, paid = 5.DOLLARS))
//                command(BOB.publicKey, fiveDollars.withNewOwner(newOwner = BOB.party).command)
//                command(listOf(ALICE.publicKey, BOB.publicKey), IOUContract.Commands.Settle())
//                this `fails with` "The borrower may not change when settling."
//            }
//            transaction {
//                input(IOUContract.IOU_CONTRACT_ID, iou)
//                input(IOUContract.IOU_CONTRACT_ID, fiveDollars)
//                output(IOUContract.IOU_CONTRACT_ID, fiveDollars.withNewOwner(newOwner = ALICE.party).ownableState)
//                output(IOUContract.IOU_CONTRACT_ID, iou.copy(amount = 0.DOLLARS, paid = 5.DOLLARS))
//                command(BOB.publicKey, fiveDollars.withNewOwner(newOwner = BOB.party).command)
//                command(listOf(ALICE.publicKey, BOB.publicKey), IOUContract.Commands.Settle())
//                this `fails with` "The amount may not change when settling."
//            }
//            transaction {
//                input(IOUContract.IOU_CONTRACT_ID, iou)
//                input(IOUContract.IOU_CONTRACT_ID, fiveDollars)
//                output(IOUContract.IOU_CONTRACT_ID, fiveDollars.withNewOwner(newOwner = ALICE.party).ownableState)
//                output(IOUContract.IOU_CONTRACT_ID, iou.copy(lender = CHARLIE.party, paid = 5.DOLLARS))
//                command(BOB.publicKey, fiveDollars.withNewOwner(newOwner = BOB.party).command)
//                command(listOf(ALICE.publicKey, BOB.publicKey), IOUContract.Commands.Settle())
//                this `fails with` "The lender may not change when settling."
//            }
//            transaction {
//                input(IOUContract.IOU_CONTRACT_ID, iou)
//                input(IOUContract.IOU_CONTRACT_ID, fiveDollars)
//                output(IOUContract.IOU_CONTRACT_ID, fiveDollars.withNewOwner(newOwner = ALICE.party).ownableState)
//                output(IOUContract.IOU_CONTRACT_ID, iou.pay(5.DOLLARS))
//                command(BOB.publicKey, fiveDollars.withNewOwner(newOwner = BOB.party).command)
//                command(listOf(ALICE.publicKey, BOB.publicKey), IOUContract.Commands.Settle())
//                verifies()
//            }
//        }
//    }

    /**
     * Task 10.
     * Both the lender and the borrower must signed an IOU issue transaction.
     * TODO: Add a constraint to the contract code that ensures this is the case.
     */
//    @Test
//    fun mustBeSignedByAllParticipants() {
//        val iou = IOUState(10.DOLLARS, ALICE.party, BOB.party)
//        val cash = createCashState(5.DOLLARS, BOB.party)
//        val cashPayment = cash.withNewOwner(newOwner = ALICE.party)
//        ledgerServices.ledger {
//            transaction {
//                input(IOUContract.IOU_CONTRACT_ID, cash)
//                input(IOUContract.IOU_CONTRACT_ID, iou)
//                output(IOUContract.IOU_CONTRACT_ID, cashPayment.ownableState)
//                command(BOB.publicKey, cashPayment.command)
//                output(IOUContract.IOU_CONTRACT_ID, iou.pay(5.DOLLARS))
//                command(listOf(ALICE.publicKey, CHARLIE.publicKey), IOUContract.Commands.Settle())
//                failsWith("Both lender and borrower together only must sign IOU settle transaction.")
//            }
//            transaction {
//                input(IOUContract.IOU_CONTRACT_ID, cash)
//                input(IOUContract.IOU_CONTRACT_ID, iou)
//                output(IOUContract.IOU_CONTRACT_ID, cashPayment.ownableState)
//                command(BOB.publicKey, cashPayment.command)
//                output(IOUContract.IOU_CONTRACT_ID, iou.pay(5.DOLLARS))
//                command(BOB.publicKey, IOUContract.Commands.Settle())
//                failsWith("Both lender and borrower together only must sign IOU settle transaction.")
//            }
//            transaction {
//                input(IOUContract.IOU_CONTRACT_ID, cash)
//                input(IOUContract.IOU_CONTRACT_ID, iou)
//                output(IOUContract.IOU_CONTRACT_ID, cashPayment.ownableState)
//                command(BOB.publicKey, cashPayment.command)
//                output(IOUContract.IOU_CONTRACT_ID, iou.pay(5.DOLLARS))
//                command(listOf(ALICE.publicKey, BOB.publicKey), IOUContract.Commands.Settle())
//                verifies()
//            }
//        }
//    }
}

