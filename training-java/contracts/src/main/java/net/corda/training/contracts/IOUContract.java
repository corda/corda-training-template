package net.corda.training.contracts;

import net.corda.core.contracts.*;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.finance.contracts.asset.Cash;
import net.corda.training.states.IOUState;

import javax.swing.plaf.nimbus.State;
import java.lang.reflect.Array;
import java.security.PublicKey;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This is the contract code which defines how the [IOUState] behaves. Looks at the unit tests in
 * [IOUContractTests] for more insight on how this contract verifies a transaction.
 */

// LegalProseReference: this is just a dummy string for the time being.

@LegalProseReference(uri = "<prose_contract_uri>")
public class IOUContract implements Contract {
    public static final String IOU_CONTRACT_ID = "net.corda.training.contracts.IOUContract";

    /**
     * The IOUContract can handle three transaction types involving [IOUState]s.
     * - Issuance: Issuing a new [IOUState] on the ledger, which is a bilateral agreement between two parties.
     * - Transfer: Re-assigning the lender/beneficiary.
     * - Settle: Fully or partially settling the [IOUState] using the Corda [Cash] contract.
     */
    public interface Commands extends CommandData {
        // Add commands here.
    }
    /**
     * The contract code for the [IOUContract].
     * The constraints are self documenting so don't require any additional explanation.
     */
    @Override
    public void verify(LedgerTransaction tx) {

        // Add contract code here.
    }

}