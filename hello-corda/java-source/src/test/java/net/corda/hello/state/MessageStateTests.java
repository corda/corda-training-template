package net.corda.hello.state;

import net.corda.core.identity.Party;
import net.corda.hello.MessageState;
import org.junit.Test;
import java.lang.reflect.Field;
import java.util.*;
import static org.junit.Assert.assertTrue;

public class MessageStateTests {

    @Test
    public void hasOriginFieldOfCorrectType() throws NoSuchFieldException {
        // Does the origin field exist?
        Field amountField = MessageState.class.getDeclaredField("origin");
        // Is the origin field of the correct type?
        assertTrue(amountField.getType().isAssignableFrom(Party.class));
    }

    @Test
    public void hasTargetFieldOfCorrectType() throws NoSuchFieldException {
        // Does the target field exist?
        Field amountField = MessageState.class.getDeclaredField("target");
        // Is the target field of the correct type?
        assertTrue(amountField.getType().isAssignableFrom(Party.class));
    }

    @Test
    public void checkMessageStateParameterOrdering() throws NoSuchFieldException {
        List<Field> fields = Arrays.asList(MessageState.class.getDeclaredFields());
        int originIdx = fields.indexOf(MessageState.class.getDeclaredField("origin"));
        int targetIdx = fields.indexOf(MessageState.class.getDeclaredField("target"));
        assertTrue(originIdx < targetIdx);
    }

}
