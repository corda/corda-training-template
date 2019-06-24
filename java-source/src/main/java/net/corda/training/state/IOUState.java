package net.corda.training.state;

import net.corda.core.contracts.*;
import net.corda.core.identity.Party;
import net.corda.core.identity.AbstractParty;

import java.math.BigDecimal;
import java.util.*;
import com.google.common.collect.ImmutableList;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.finance.Currencies;
import net.corda.training.contract.IOUContract;
import org.jetbrains.annotations.NotNull;

/**
 * This is where you'll add the definition of your state object. Look at the unit tests in [IOUStateTests] for
 * instructions on how to complete the [IOUState] class.
 *
 */
@BelongsToContract(IOUContract.class)
public class IOUState implements LinearState {

    private Amount<Currency> amount;
    private Party lender, borrower;
    private Amount<Currency> paid;
    private UniqueIdentifier linearId;

    public IOUState(Amount<Currency> amount, Party lender, Party borrower) {
        this.amount = amount;
        this.lender = lender;
        this.borrower = borrower;
        this.paid = new Amount<>(0, amount.getDisplayTokenSize(), amount.getToken());
        this.linearId = new UniqueIdentifier();
    }

    @ConstructorForDeserialization
    private IOUState(Amount<Currency> amount, Party lender, Party borrower, Amount<Currency> paid, UniqueIdentifier linearId) {
        this.amount = amount;
        this.lender = lender;
        this.borrower = borrower;
        this.paid = paid;
        this.linearId = linearId;
    }

    public IOUState pay(Amount<Currency> amountToPay) {
        return new IOUState(amount, lender, borrower, paid.plus(amountToPay), linearId);
    }

    public IOUState withNewLender(Party newLender) {
        return new IOUState(amount, newLender, borrower, paid, linearId);
    }

    public IOUState copy(Amount<Currency> newAmount, Party newLender, Party newBorrower, Amount<Currency> newPaid) {
        return new IOUState(newAmount, newLender, newBorrower, newPaid, linearId);
    }

    public boolean equals(IOUState otherState) {
        if (!amount.equals(otherState.getAmount())) return false;
        if (!lender.equals(otherState.getLender())) return false;
        if (!borrower.equals(otherState.getBorrower())) return false;
        if (!paid.equals(otherState.getPaid())) return false;
        if (!linearId.equals(otherState.getLinearId())) return false;
        return true;
    }

    public Amount<Currency> getPaid() {
        return paid;
    }
    public Party getLender() {
        return lender;
    }
    public Party getBorrower() {
        return borrower;
    }
    public Amount<Currency> getAmount() { return amount; }

    /**
     *  This method will return a list of the nodes which can "use" this state in a valid transaction. In this case, the
     *  lender or the borrower.
     */
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(lender, borrower);
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }
}