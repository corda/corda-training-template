package net.corda.training.contract

import net.corda.contracts.asset.Cash
import net.corda.core.contracts.*
import net.corda.core.crypto.CompositeKey
import net.corda.core.serialization.OpaqueBytes
import net.corda.testing.*
import net.corda.training.state.IOUState
import org.junit.Test
import java.util.*

/**
 * Practical exercise instructions.
 * The objective here is to write some contract code that verifies a transaction to settle an [IOUState].
 * Settling is more complicated than transfering and issuing as it requires you to use multiple state types in a
 * transaction.
 * As with the [IOUIssueTests] and [IOUTransferTests] uncomment each unit test and run them one at a time. Use the body
 * of the tests and the task description to determine how to get the tests to pass.
 */
class IOUSettleTests {
    val defaultRef = OpaqueBytes(ByteArray(1, { 1 }))
    val defaultIssuer = MEGA_CORP.ref(defaultRef)
    val iou = IOUState(10.POUNDS, ALICE, BOB)

    private fun createCashState(amount: Amount<Currency>, owner: CompositeKey): Cash.State {
        return Cash.State(amount = amount `issued by` defaultIssuer, owner = owner)
    }

    val dummyState = object : ContractState {
        override val contract get() = DUMMY_PROGRAM_ID
        override val participants: List<CompositeKey> get() = listOf()
    }
    // A pre-defined dummy command.=
    class DummyCommand : TypeOnlyCommandData()

    /**
     * Task 1.
     * We need to add another case to deal with settling in the [IOUContract.verify] function.
     * TODO: Add the [IOUContract.Commands.Settle] case to the verify function.
     * Hint: You can leave the body empty for now.
     */
    @Test
    fun mustIncludeSettleCommand() {
        val inputCash = createCashState(5.POUNDS, BOB_PUBKEY)
        val outputCash = inputCash.withNewOwner(newOwner = ALICE_PUBKEY).second
        ledger {
            transaction {
                input { iou }
                output { iou.pay(5.POUNDS) }
                input { inputCash }
                output { outputCash }
                command(BOB_PUBKEY) { Cash.Commands.Move() }
                this.fails()
            }
            transaction {
                input { iou }
                output { iou.pay(5.POUNDS) }
                input { inputCash }
                output { outputCash }
                command(BOB_PUBKEY) { Cash.Commands.Move() }
                command(ALICE_PUBKEY, BOB_PUBKEY) { DummyCommand() } // Wrong type.
                this.fails()
            }
            transaction {
                input { iou }
                output { iou.pay(5.POUNDS) }
                input { inputCash }
                output { outputCash }
                command(BOB_PUBKEY) { Cash.Commands.Move() }
                command(ALICE_PUBKEY, BOB_PUBKEY) { IOUContract.Commands.Settle() } // Correct Type.
                this.verifies()
            }
        }
    }

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
    @Test
    fun mustBeOneGroupOfIOUs() {
        val iouOne = IOUState(10.POUNDS, ALICE, BOB)
        val iouTwo = IOUState(5.POUNDS, ALICE, BOB)
        val inputCash = createCashState(5.POUNDS, BOB_PUBKEY)
        val outputCash = inputCash.withNewOwner(newOwner = ALICE_PUBKEY).second
        ledger {
            transaction {
                input { iouOne }
                input { iouTwo }
                command(ALICE_PUBKEY, BOB_PUBKEY) { IOUContract.Commands.Settle() }
                output { iouOne.pay(5.POUNDS) }
                input { inputCash }
                output { outputCash }
                command(BOB_PUBKEY) { Cash.Commands.Move() }
                this `fails with` "List has more than one element."
            }
            transaction {
                input { iouOne }
                command(ALICE_PUBKEY, BOB_PUBKEY) { IOUContract.Commands.Settle() }
                output { iouOne.pay(5.POUNDS) }
                input { inputCash }
                output { outputCash }
                command(BOB_PUBKEY) { Cash.Commands.Move() }
                this.verifies()
            }
        }
    }

    /**
     * Task 3.
     * There always has to be one input IOU ina settle transaction but there might not be an output IOU.
     * TODO: Add a constraint to check there is always one input IOU.
     */
    @Test
    fun mustHaveOneInputIOU() {
        val iouOne = IOUState(10.POUNDS, ALICE, BOB)
        val tenPounds = createCashState(10.POUNDS, BOB_PUBKEY)
        val fivePounds = createCashState(5.POUNDS, BOB_PUBKEY)
        ledger {
            transaction {
                command(ALICE_PUBKEY, BOB_PUBKEY) { IOUContract.Commands.Settle() }
                output { iou }
                this `fails with` "There must be one input IOU."
            }
            transaction {
                input { iouOne }
                command(ALICE_PUBKEY, BOB_PUBKEY) { IOUContract.Commands.Settle() }
                output { iouOne.pay(5.POUNDS) }
                input { fivePounds }
                output { fivePounds.withNewOwner(newOwner = ALICE_PUBKEY).second }
                command(BOB_PUBKEY) { Cash.Commands.Move() }
                this.verifies()
            }
            transaction {
                input { iouOne }
                command(ALICE_PUBKEY, BOB_PUBKEY) { IOUContract.Commands.Settle() }
                input { tenPounds }
                output { tenPounds.withNewOwner(newOwner = ALICE_PUBKEY).second }
                command(BOB_PUBKEY) { Cash.Commands.Move() }
                this.verifies()
            }
        }
    }

    /**
     * Task 4.
     * Now we need to ensure that there are cash states present in the outputs list. The [IOUContract] doesn't care
     * about input cash as the validity of the cash transaction will be checked by the [Cash] contract. We do however
     * need to count how much cash is being used to settle and update our [IOUState] accordingly.
     * TODO: Filter out the cash states from the list of outputs list and assign them to a constant.
     * Hint:
     * - Use the [filterIsInstance] extension function to filter the outputs list by type, in this case [Cash.State].
     */
    @Test
    fun mustBeCashOutputStatesPresent() {
        val iou = IOUState(10.DOLLARS, ALICE, BOB)
        val cash = createCashState(5.DOLLARS, BOB_PUBKEY)
        val cashPayment = cash.withNewOwner(newOwner = ALICE_PUBKEY)
        ledger {
            transaction {
                input { iou }
                output { iou.pay(5.DOLLARS) }
                command(ALICE_PUBKEY, BOB_PUBKEY) { IOUContract.Commands.Settle() }
                this `fails with` "There must be output cash."
            }
            transaction {
                input { iou }
                input { cash }
                output { iou.pay(5.DOLLARS) }
                output { cashPayment.second }
                command(BOB_PUBKEY) { cashPayment.first }
                command(ALICE_PUBKEY, BOB_PUBKEY) { IOUContract.Commands.Settle() }
                this.verifies()
            }
        }
    }

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
    @Test
    fun mustBeCashOutputStatesWithRecipientAsOwner() {
        val iou = IOUState(10.POUNDS, ALICE, BOB)
        val cash = createCashState(5.POUNDS, BOB_PUBKEY)
        val invalidCashPayment = cash.withNewOwner(newOwner = CHARLIE_PUBKEY)
        val validCashPayment = cash.withNewOwner(newOwner = ALICE_PUBKEY)
        ledger {
            transaction {
                input { iou }
                input { cash }
                output { iou.pay(5.POUNDS) }
                output { invalidCashPayment.second }
                command(BOB_PUBKEY) { invalidCashPayment.first }
                command(ALICE_PUBKEY, BOB_PUBKEY) { IOUContract.Commands.Settle() }
                this `fails with` "There must be output cash paid to the recipient."
            }
            transaction {
                input { iou }
                input { cash }
                output { iou.pay(5.POUNDS) }
                output { validCashPayment.second }
                command(BOB_PUBKEY) { validCashPayment.first }
                command(ALICE_PUBKEY, BOB_PUBKEY) { IOUContract.Commands.Settle() }
                this.verifies()
            }
        }
    }

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
    @Test
    fun cashSettlementAmountMustBeLessThanRemainingIOUAmount() {
        val iou = IOUState(10.DOLLARS, ALICE, BOB)
        val elevenDollars = createCashState(11.DOLLARS, BOB_PUBKEY)
        val tenDollars = createCashState(10.DOLLARS, BOB_PUBKEY)
        val fiveDollars = createCashState(5.DOLLARS, BOB_PUBKEY)
        ledger {
            transaction {
                input { iou }
                input { elevenDollars }
                output { iou.pay(11.DOLLARS) }
                output { elevenDollars.withNewOwner(newOwner = ALICE_PUBKEY).second }
                command(BOB_PUBKEY) { elevenDollars.withNewOwner(newOwner = ALICE_PUBKEY).first }
                command(ALICE_PUBKEY, BOB_PUBKEY) { IOUContract.Commands.Settle() }
                this `fails with` "The amount settled cannot be more than the amount outstanding."
            }
            transaction {
                input { iou }
                input { fiveDollars }
                output { iou.pay(5.DOLLARS) }
                output { fiveDollars.withNewOwner(newOwner = ALICE_PUBKEY).second }
                command(BOB_PUBKEY) { fiveDollars.withNewOwner(newOwner = ALICE_PUBKEY).first }
                command(ALICE_PUBKEY, BOB_PUBKEY) { IOUContract.Commands.Settle() }
                this.verifies()
            }
            transaction {
                input { iou }
                input { tenDollars }
                output { tenDollars.withNewOwner(newOwner = ALICE_PUBKEY).second }
                command(BOB_PUBKEY) { tenDollars.withNewOwner(newOwner = ALICE_PUBKEY).first }
                command(ALICE_PUBKEY, BOB_PUBKEY) { IOUContract.Commands.Settle() }
                this.verifies()
            }
        }
    }

    /**
     * Task 7.
     * Kotlin's type system should handle this for you but it goes without saying that we should only be able to settle
     * in the currency that the IOU in denominated in.
     * TODO: You shouldn't have anything to do here but here are some tests just to make sure!
     */
    @Test
    fun cashSettlementMustBeInTheCorrectCurrency() {
        val iou = IOUState(10.DOLLARS, ALICE, BOB)
        val tenDollars = createCashState(10.DOLLARS, BOB_PUBKEY)
        val tenPounds = createCashState(10.POUNDS, BOB_PUBKEY)
        ledger {
            transaction {
                input { iou }
                input { tenPounds }
                output { tenPounds.withNewOwner(newOwner = ALICE_PUBKEY).second }
                command(BOB_PUBKEY) { tenPounds.withNewOwner(newOwner = ALICE_PUBKEY).first }
                command(ALICE_PUBKEY, BOB_PUBKEY) { IOUContract.Commands.Settle() }
                this `fails with` "Token mismatch: GBP vs USD"
            }
            transaction {
                input { iou }
                input { tenDollars }
                output { tenDollars.withNewOwner(newOwner = ALICE_PUBKEY).second }
                command(BOB_PUBKEY) { tenDollars.withNewOwner(newOwner = ALICE_PUBKEY).first }
                command(ALICE_PUBKEY, BOB_PUBKEY) { IOUContract.Commands.Settle() }
                this.verifies()
            }
        }
    }

    /**
     * Task 8.
     * If we fully settle the IOU, then we are done and thus don't require one on ledger anymore. However, if we only
     * partially settle the IOU, then we want to keep the IOU on ledger with an amended [paid] property.
     * TODO: Write a constraint that ensures the correct behaviour depending on the amount settled vs amount remaining.
     * Hint: You can use a simple if statement and compare the total amount paid vs amount left to settle.
     */
    @Test
    fun mustOnlyHaveOutputIOUIfNotFullySettling() {
        val iou = IOUState(10.DOLLARS, ALICE, BOB)
        val tenDollars = createCashState(10.DOLLARS, BOB_PUBKEY)
        val fiveDollars = createCashState(5.DOLLARS, BOB_PUBKEY)
        ledger {
            transaction {
                input { iou }
                input { fiveDollars }
                output { fiveDollars.withNewOwner(newOwner = ALICE_PUBKEY).second }
                command(BOB_PUBKEY) { fiveDollars.withNewOwner(newOwner = BOB_PUBKEY).first }
                command(ALICE_PUBKEY, BOB_PUBKEY) { IOUContract.Commands.Settle() }
                this `fails with` "There must be one output IOU."
            }
            transaction {
                input { iou }
                input { fiveDollars }
                output { fiveDollars.withNewOwner(newOwner = ALICE_PUBKEY).second }
                output { iou.pay(5.DOLLARS) }
                command(BOB_PUBKEY) { fiveDollars.withNewOwner(newOwner = BOB_PUBKEY).first }
                command(ALICE_PUBKEY, BOB_PUBKEY) { IOUContract.Commands.Settle() }
                verifies()
            }
            transaction {
                input { tenDollars }
                input { iou }
                output { iou.pay(10.DOLLARS) }
                output { tenDollars.withNewOwner(newOwner = ALICE_PUBKEY).second }
                command(BOB_PUBKEY) { tenDollars.withNewOwner(newOwner = BOB_PUBKEY).first }
                command(ALICE_PUBKEY, BOB_PUBKEY) { IOUContract.Commands.Settle() }
                this `fails with` "There must be no output IOU as it has been fully settled."
            }
            transaction {
                input { tenDollars }
                input { iou }
                output { tenDollars.withNewOwner(newOwner = ALICE_PUBKEY).second }
                command(BOB_PUBKEY) { tenDollars.withNewOwner(newOwner = BOB_PUBKEY).first }
                command(ALICE_PUBKEY, BOB_PUBKEY) { IOUContract.Commands.Settle() }
                verifies()
            }
        }
    }

    /**
     * Task 9.
     * We want to make sure that the only property of the IOU which changes when we settle, is the paid amount.
     * TODO: Write a constraint to check only the paid property of the [IOUState] changes when settling.
     */
    @Test
    fun onlyPaidPropertyMayChange() {
        val iou = IOUState(10.DOLLARS, ALICE, BOB)
        val fiveDollars = createCashState(5.DOLLARS, BOB_PUBKEY)
        ledger {
            transaction {
                input { iou }
                input { fiveDollars }
                output { fiveDollars.withNewOwner(newOwner = ALICE_PUBKEY).second }
                output { iou.copy(borrower = ALICE, paid = 5.DOLLARS) }
                command(BOB_PUBKEY) { fiveDollars.withNewOwner(newOwner = BOB_PUBKEY).first }
                command(ALICE_PUBKEY, BOB_PUBKEY) { IOUContract.Commands.Settle() }
                this `fails with` "The borrower may not change when settling."
            }
            transaction {
                input { iou }
                input { fiveDollars }
                output { fiveDollars.withNewOwner(newOwner = ALICE_PUBKEY).second }
                output { iou.copy(amount = 0.DOLLARS, paid = 5.DOLLARS) }
                command(BOB_PUBKEY) { fiveDollars.withNewOwner(newOwner = BOB_PUBKEY).first }
                command(ALICE_PUBKEY, BOB_PUBKEY) { IOUContract.Commands.Settle() }
                this `fails with` "The amount may not change when settling."
            }
            transaction {
                input { iou }
                input { fiveDollars }
                output { fiveDollars.withNewOwner(newOwner = ALICE_PUBKEY).second }
                output { iou.copy(lender = CHARLIE, paid = 5.DOLLARS) }
                command(BOB_PUBKEY) { fiveDollars.withNewOwner(newOwner = BOB_PUBKEY).first }
                command(ALICE_PUBKEY, BOB_PUBKEY) { IOUContract.Commands.Settle() }
                this `fails with` "The lender may not change when settling."
            }
            transaction {
                input { iou }
                input { fiveDollars }
                output { fiveDollars.withNewOwner(newOwner = ALICE_PUBKEY).second }
                output { iou.pay(5.DOLLARS) }
                command(BOB_PUBKEY) { fiveDollars.withNewOwner(newOwner = BOB_PUBKEY).first }
                command(ALICE_PUBKEY, BOB_PUBKEY) { IOUContract.Commands.Settle() }
                verifies()
            }
        }
    }

    /**
     * Task 10.
     * Both the lender and the borrower must signed an IOU issue transaction.
     * TODO: Add a constraint to the contract code that ensures this is the case.
     */
    @Test
    fun mustBeSignedByAllParticipants() {
        val iou = IOUState(10.DOLLARS, ALICE, BOB)
        val cash = createCashState(5.DOLLARS, BOB_PUBKEY)
        val cashPayment = cash.withNewOwner(newOwner = ALICE_PUBKEY)
        ledger {
            transaction {
                input { cash }
                input { iou }
                output { cashPayment.second }
                command(BOB_PUBKEY) { cashPayment.first }
                output { iou.pay(5.DOLLARS) }
                command(ALICE_PUBKEY, CHARLIE_PUBKEY) { IOUContract.Commands.Settle() }
                failsWith("Both lender and borrower together only must sign IOU settle transaction.")
            }
            transaction {
                input { cash }
                input { iou }
                output { cashPayment.second }
                command(BOB_PUBKEY) { cashPayment.first }
                output { iou.pay(5.DOLLARS) }
                command(BOB_PUBKEY) { IOUContract.Commands.Settle() }
                failsWith("Both lender and borrower together only must sign IOU settle transaction.")
            }
            transaction {
                input { cash }
                input { iou }
                output { cashPayment.second }
                command(BOB_PUBKEY) { cashPayment.first }
                output { iou.pay(5.DOLLARS) }
                command(ALICE_PUBKEY, BOB_PUBKEY) { IOUContract.Commands.Settle() }
                verifies()
            }
        }
    }
}

