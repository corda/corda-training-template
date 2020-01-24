package net.corda.hello.contract;

import net.corda.core.contracts.*;
import net.corda.hello.state.MessageStateTests;
import net.corda.testing.node.MockServices;
import net.corda.core.transactions.LedgerTransaction;
import org.junit.Test;

import java.util.Arrays;

public class SendMessageTests {
    // A pre-defined dummy command.
    public interface Commands extends CommandData {
        class DummyCommand extends TypeOnlyCommandData implements Commands{}
    }

    static private final MockServices ledgerServices = new MockServices(
            Arrays.asList("net.corda.hello", "net.corda.finance.contracts")
    );

}
