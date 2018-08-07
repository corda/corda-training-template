package net.corda.training.state;

import net.corda.core.contracts.ContractState;
import net.corda.core.identity.Party;
import net.corda.core.identity.AbstractParty;

import java.util.*;
import com.google.common.collect.ImmutableList;

/**
 * This is where you'll add the definition of your state object. Look at the unit tests in [IOUStateTests] for
 * instructions on how to complete the [IOUState] class.
 *
 */
public class IOUState implements ContractState {

	public IOUState(){

	}

   	@Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of();
    }
}