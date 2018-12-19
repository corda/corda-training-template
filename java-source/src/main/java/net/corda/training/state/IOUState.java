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

    public IOUState() {}

    @Override
    public UniqueIdentifier getLinearId() {
        return new UniqueIdentifier();
    }

    /**
     *  This method will return a list of the nodes which can "use" this state in a valid transaction. In this case, the
     *  lender or the borrower.
     */
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of();
    }

}