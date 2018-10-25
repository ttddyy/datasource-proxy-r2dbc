package net.ttddyy.dsproxy.r2dbc;

import net.ttddyy.dsproxy.r2dbc.core.MethodExecutionInfo;
import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Tadaya Tsuyukubo
 */
public class MethodExecutionInfoFormatterTest {

    @Test
    void withDefault() {

        // String#indexOf(int) method
        Method method = ReflectionUtils.findMethod(String.class, "indexOf", int.class);

        Long target = 100L;

        MethodExecutionInfo executionInfo = new MethodExecutionInfo();
        executionInfo.setThreadId(5);
        executionInfo.setConnectionId("ABC");
        executionInfo.setExecuteDuration(Duration.of(23, ChronoUnit.MILLIS));
        executionInfo.setMethod(method);
        executionInfo.setTarget(target);

        MethodExecutionInfoFormatter formatter = MethodExecutionInfoFormatter.withDefault();
        String result = formatter.format(executionInfo);

        assertEquals("  1: Thread:5 Connection:ABC Time:23  Long#indexOf()", result);

        // second time should increase the sequence
        result = formatter.format(executionInfo);
        assertEquals("  2: Thread:5 Connection:ABC Time:23  Long#indexOf()", result);

    }

    @Test
    void customConsumer() {

        MethodExecutionInfoFormatter formatter = new MethodExecutionInfoFormatter();
        formatter.addConsumer((executionInfo, sb) -> {
            sb.append("ABC");
        });
        String result = formatter.format(new MethodExecutionInfo());

        assertEquals("ABC", result);

    }

}
