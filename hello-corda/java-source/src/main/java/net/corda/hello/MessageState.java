package net.corda.hello;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import java.util.List;


/** TODO(#1):
 Update this code so that the MessageState constructor takes an additional parameter 'content' of type String.
 This parameter should set a local parameter that is also named ‘content'.
 **/

@BelongsToContract(MessageContract.class)
public class MessageState implements ContractState {
    public final Party origin;
    public final Party target;
    public MessageState(Party origin, Party target) {
        this.origin = origin;
        this.target = target;
    }
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(origin, target);
    }
}
