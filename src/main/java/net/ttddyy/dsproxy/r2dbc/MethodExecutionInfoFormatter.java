package net.ttddyy.dsproxy.r2dbc;

import net.ttddyy.dsproxy.r2dbc.core.MethodExecutionInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Convert {@link MethodExecutionInfo} to {@link String}.
 *
 * @author Tadaya Tsuyukubo
 */
public class MethodExecutionInfoFormatter implements Function<MethodExecutionInfo, String> {

    private static final String DEFAULT_DELIMITER = " ";

    private List<BiConsumer<MethodExecutionInfo, StringBuilder>> consumers = new ArrayList<>();

    private AtomicLong sequenceNumber = new AtomicLong(1);

    // Default consumer to format the MethodExecutionInfo
    private BiConsumer<MethodExecutionInfo, StringBuilder> defaultConsumer = (executionInfo, sb) -> {
        long seq = this.sequenceNumber.getAndIncrement();
        String connectionId = executionInfo.getConnectionId();
        long executionTime = executionInfo.getExecuteDuration().toMillis();
        String targetClass = executionInfo.getTarget().getClass().getSimpleName();
        String methodName = executionInfo.getMethod().getName();
        long threadId = executionInfo.getThreadId();

        sb.append(String.format("%3d: Thread:%d Connection:%s Time:%d  %s#%s()",
                seq, threadId, connectionId, executionTime, targetClass, methodName));
    };

    private String delimiter = DEFAULT_DELIMITER;


    public static MethodExecutionInfoFormatter withDefault() {
        MethodExecutionInfoFormatter formatter = new MethodExecutionInfoFormatter();
        formatter.addConsumer(formatter.defaultConsumer);
        return formatter;
    }

    @Override
    public String apply(MethodExecutionInfo executionInfo) {
        return format(executionInfo);
    }

    public String format(MethodExecutionInfo executionInfo) {

        StringBuilder sb = new StringBuilder();

        consumers.forEach(consumer -> {
            consumer.accept(executionInfo, sb);
            sb.append(this.delimiter);
        });

        chompIfEndWith(sb, this.delimiter);

        return sb.toString();

    }

    public MethodExecutionInfoFormatter addConsumer(BiConsumer<MethodExecutionInfo, StringBuilder> consumer) {
        this.consumers.add(consumer);
        return this;
    }

    // TODO: share this with ExecutionInfoFormatter
    protected void chompIfEndWith(StringBuilder sb, String s) {
        if (sb.length() < s.length()) {
            return;
        }
        final int startIndex = sb.length() - s.length();
        if (sb.substring(startIndex, sb.length()).equals(s)) {
            sb.delete(startIndex, sb.length());
        }
    }


}
