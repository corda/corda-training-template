package net.corda.training.state

import net.corda.core.contracts.*
import net.corda.core.crypto.Party
import net.corda.core.crypto.AnonymousParty
import net.corda.core.crypto.CompositeKey
import net.corda.testing.ALICE
import net.corda.testing.ALICE_PUBKEY
import net.corda.testing.BOB
import net.corda.testing.BOB_PUBKEY
import net.corda.training.contract.IOUContract
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Practical exercise instructions.
 * Uncomment the first unit test [hasIOUAmountFieldOfCorrectType] then run the unit test using the green arrow
 * to the left of the [IOUStateTests] class or the [hasIOUAmountFieldOfCorrectType] method.
 * Running the unit tests from [IOUStateTests] runs all of the unit tests defined in the class.
 * The test should fail because you need to make some changes to the IOUState to make the test pass. Read the TODO
 * under each task number for a description and a hint of what you need to do.
 * Once you have the unit test passing, uncomment the next test.
 * Continue until all the unit tests pass.
 * Hint: CMD / Ctrl + click on the brown type names in square brackets for that type's definition in the codebase.
 */
class IOUStateTests {
    // An IOUState we can use for the unit tests.
    // In this IOU, Alice is lending £1 to Bob.
    /**
     * Task 1.
     * TODO: Add an 'amount' property of type [Amount] to the [IOUState] class to get this test to pass.
     * Hint: [Amount] is a template class that takes a class parameter of the token you would like an [Amount] of.
     * As we are dealing with cash lent from one Party to another a sensible token to use would be [Currency].
     */
//    @Test
//    fun hasIOUAmountFieldOfCorrectType() {
//        // Does the amount field exist?
//        IOUState::class.java.getDeclaredField("amount")
//        // Is the amount field of the correct type?
//        assertEquals(IOUState::class.java.getDeclaredField("amount").type, Amount::class.java)
//    }

    /**
     * Task 2.
     * TODO: Add a 'lender' property of type [Party] to the [IOUState] class to get this test to pass.
     */
//    @Test
//    fun hasLenderFieldOfCorrectType() {
//        // Does the lender field exist?
//        IOUState::class.java.getDeclaredField("lender")
//        // Is the lender field of the correct type?
//        assertEquals(IOUState::class.java.getDeclaredField("lender").type, Party::class.java)
//    }

    /**
     * Task 3.
     * TODO: Add a 'borrower' property of type [Party] to the [IOUState] class to get this test to pass.
     */
//    @Test
//    fun hasBorrowerFieldOfCorrectType() {
//        // Does the borrower field exist?
//        IOUState::class.java.getDeclaredField("borrower")
//        // Is the borrower field of the correct type?
//        assertEquals(IOUState::class.java.getDeclaredField("borrower").type, Party::class.java)
//    }

    /**
     * Task 4.
     * TODO: Add an 'paid' property of type [Amount] to the [IOUState] class to get this test to pass.
     * Hint:
     * - We would like this property to be initialised to a zero amount of Currency upon creation of the [IOUState].
     * - You can use the [POUNDS] extension function over [Int] to create an amount of pounds e.g. '10.POUNDS'.
     * - This property keeps track of how much of the initial [IOUState.amount] has been settled by the borrower.
     * - Extra credit: We need to make sure that the [IOUState.paid] property is of the same currency type as the
     *   [IOUState.amount] property. You can create an instance of the [Amount] class that takes a zero value and a token
     *   representing the currency - which should be the same currency as the [IOUState.amount] property.
     * - You can initialise a property with a default value in a Kotlin data class like this:
     *
     *       data class(val number: Int = 10)
     */
//    @Test
//    fun hasPaidFieldOfCorrectType() {
//        // Does the paid field exist?
//        IOUState::class.java.getDeclaredField("paid")
//        // Is the paid field of the correct type?
//        assertEquals(IOUState::class.java.getDeclaredField("paid").type, Amount::class.java)
//        // Does the paid field's currency match the amount field's currency?
//        val iouStateGBP = IOUState(1.POUNDS, ALICE, BOB)
//        val iouStateUSD = IOUState(1.DOLLARS, ALICE, BOB)
//        assertEquals(iouStateGBP.amount.token, iouStateGBP.paid.token)
//        assertEquals(iouStateUSD.amount.token, iouStateUSD.paid.token)
//    }

    /**
     * Task 5.
     * TODO: Add an entry to the [IOUState.participants] list for the lender.
     * Hint: Note that [IOUState.participants] list is a list of [CompositeKey]s. [Party] or [AnonymousParty] objects
     * include a property which contains their [CompositeKey] called [Party.owningKey]
     */
//    @Test
//    fun lenderIsParticipant() {
//        val iouState = IOUState(1.POUNDS, ALICE, BOB)
//        assertNotEquals(iouState.participants.indexOf(ALICE_PUBKEY), -1)
//    }

    /**
     * Task 6.
     * TODO: Similar to the last task, add an entry to the [IOUState.participants] list for the borrower.
     * Hint: Note that [IOUState.participants] list is a list of [CompositeKey]s. [AnonymousParty] objects include a
     * property which contains their [CompositeKey] called [Party.owningKey]
     */
//    @Test
//    fun borrowerIsParticipant() {
//        val iouState = IOUState(1.POUNDS, ALICE, BOB)
//        assertNotEquals(iouState.participants.indexOf(BOB_PUBKEY), -1)
//    }

    /**
     * Task 7.
     * TODO: Implement [LinearState] along with the required properties and methods.
     * Hint: [LinearState] implements [ContractState] which defines an additional property and method. You can use
     * IntellIJ to automatically add the member definitions for you or you can add them yourself. Look at the definition
     * of [LinearState] for what requires adding.
     */
//    @Test
//    fun isLinearState() {
//        assert(LinearState::class.java.isAssignableFrom(IOUState::class.java))
//    }

    /**
     * Task 8.
     * TODO: Override the [LinearState.linearId] property and assign it a value via your state's constructor.
     * Hint: The [LinearState.linearId] property is of type [UniqueIdentifier]. You need to create a new instance of
     * the [UniqueIdentifier] class.
     * The [LinearState.linearId] is designed to link all [LinearState]s (which represent the state of an
     * agreement at a specific point in time) together. All the [LinearState]s with the same [LinearState.linearId]
     * represent the complete life-cycle to date of an agreement, asset or shared fact.
     */
//    @Test
//    fun hasLinearIdFieldOfCorrectType() {
//        // Does the linearId field exist?
//        IOUState::class.java.getDeclaredField("linearId")
//        // Is the paid field of the correct type?
//        assertEquals(IOUState::class.java.getDeclaredField("linearId").type, UniqueIdentifier::class.java)
//    }

    /**
     * Task 9.
     * TODO: Complete the [IOUState.isRelevant] method.
     * Hint: The [LinearState.isRelevant] method is used by the Corda vault of a node to ascertain if the state
     * implementing the method is one that the calling node's vault should be tracking. For the [IOUState] this is
     * performed based on whether the public keys of the lender or borrower are known to the node, i.e. if the node IS
     * the lender or borrower.
     * We can do this by performing a set intersection of the vault public keys (which are passed in as a
     * parameter to the method) with the [IOUState.participants] public keys and check the result is not the empty set.
     * You can use the [participants.keys] method to get a list of all the [CompositeKeys].
     * You can use [isNotEmpty] to check the intersection of the sets is not empty!
     */
//    @Test
//    fun isRelevantMethodComplete() {
//        val iouState = IOUState(1.POUNDS, ALICE, BOB)
//        assert(iouState.isRelevant(setOf(ALICE.owningKey.singleKey, BOB.owningKey.singleKey)))
//    }

    /**
     * Task 10.
     * TODO: Ensure parameters are ordered correctly.
     * Hint: Make sure that the lender and borrower fields are not in the wrong order as this may cause some
     * confusion in subsequent tasks!
     */
//    @Test
//    fun checkIOUStateParameterOrdering() {
//        val fields = IOUState::class.java.declaredFields
//        assertEquals(fields[0], IOUState::class.java.getDeclaredField("amount"))
//        assertEquals(fields[1], IOUState::class.java.getDeclaredField("lender"))
//        assertEquals(fields[2], IOUState::class.java.getDeclaredField("borrower"))
//        assertEquals(fields[3], IOUState::class.java.getDeclaredField("paid"))
//        assertEquals(fields[4], IOUState::class.java.getDeclaredField("linearId"))
//    }

    /**
     * Task 11.
     * TODO: Override the toString() method for the [IOUState]. See unit test body for specifics of the format.
     * Hint: You can use string interpolation in Kotlin to easily create strings using previously declared variables.
     * For example:
     *
     *     val str: String = "World"
     *     println("Hello $str")
     *
     * More information: https://kotlinlang.org/docs/reference/basic-types.html#string-templates
     *
     * Methods are defined as "fun foo(param: ParamType): ReturnType { }" in Kotlin or if the body is a single
     * expression, you can use "fun foo(param: ParamType): ReturnType = expression"
     *
     * More information: https://kotlinlang.org/docs/reference/functions.html#function-declarations
     *
     * The name string name of a party can be obtained from the following property [Party.name].
     */
//    @Test
//    fun checkIOUStateToStringMethod() {
//        val iouState = IOUState(1.POUNDS, ALICE, BOB)
//        assertEquals(iouState.toString(), "IOU(${iouState.linearId}): Bob owes Alice 1.00 GBP and has paid 0.00 GBP so far.")
//    }

    /**
     * Task 12.
     * TODO: Add a helper method called [pay] that can be called from an [IOUState] to settle an amount of the IOU.
     * Hint: You will need to increase the [IOUState.paid] property by the amount the borrower wishes to pay.
     */
//    @Test
//    fun checkPayHelperMethod() {
//        val iou = IOUState(10.DOLLARS, ALICE, BOB)
//        assertEquals(5.DOLLARS, iou.pay(5.DOLLARS).paid)
//        assertEquals(3.DOLLARS, iou.pay(1.DOLLARS).pay(2.DOLLARS).paid)
//        assertEquals(10.DOLLARS, iou.pay(5.DOLLARS).pay(3.DOLLARS).pay(2.DOLLARS).paid)
//    }
}
