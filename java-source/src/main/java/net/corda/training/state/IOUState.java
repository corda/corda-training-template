package net.corda.training.state;

import net.corda.core.contracts.Amount;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import net.corda.core.identity.AbstractParty;

import java.util.*;
import com.google.common.collect.ImmutableList;
import net.corda.core.serialization.ConstructorForDeserialization;

import javax.validation.constraints.NotNull;

/**
 * This is where you'll add the definition of your state object. Look at the unit tests in [IOUStateTests] for
 * instructions on how to complete the [IOUState] class.
 *
 */
public class IOUState implements ContractState, LinearState {

    public final Amount<Currency> amount;
    public final Party lender;
    public final Party borrower;
    public final Amount<Currency> paid;
    private final UniqueIdentifier linearId;

    // Private constructor used only for copying a State object
    @ConstructorForDeserialization
    private IOUState(Amount<Currency> amount, Party lender, Party borrower, Amount<Currency> paid, UniqueIdentifier linearId){
       this.amount = amount;
       this.lender = lender;
       this.borrower = borrower;
       this.paid = paid;
       this.linearId = linearId;
	}

	public IOUState(Amount<Currency> amount, Party lender, Party borrower) {
        this(amount, lender, borrower, new Amount<>(0, amount.getToken()), new UniqueIdentifier());
    }

    public Amount<Currency> getAmount() {
        return amount;
    }

    public Party getLender() {
	    return lender;
    }

    public Party getBorrower() {
        return lender;
    }

    public Amount getPaid() {
	    return paid;
    }

    @Override
    public UniqueIdentifier getLinearId() {
	    return linearId;
    }

   	@Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(borrower, lender);
    }

    public IOUState pay(Amount paidAmount) {
        Amount<Currency> newAmountPaid = this.paid.plus(paidAmount);
        return new IOUState(amount, lender, borrower, newAmountPaid, linearId);
    }

    public IOUState withNewLender(Party newLender) {
        return new IOUState(amount, newLender, borrower, paid, linearId);
    }

}