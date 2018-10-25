package net.ttddyy.dsproxy.r2dbc;

import net.ttddyy.dsproxy.r2dbc.core.Binding;
import net.ttddyy.dsproxy.r2dbc.core.BindingValue;
import net.ttddyy.dsproxy.r2dbc.core.BindingValue.NullBindingValue;
import net.ttddyy.dsproxy.r2dbc.core.QueryExecutionInfo;
import net.ttddyy.dsproxy.r2dbc.core.ExecutionType;
import net.ttddyy.dsproxy.r2dbc.core.QueryInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;

/**
 * Convert {@link QueryExecutionInfo} to {@code String}.
 *
 * @author Tadaya Tsuyukubo
 */
public class ExecutionInfoFormatter implements Function<QueryExecutionInfo, String> {

    private static final String DEFAULT_DELIMITER = " ";

    public static final BiConsumer<QueryExecutionInfo, StringBuilder> DEFAULT_ON_THREAD = (executionInfo, sb) -> {
        sb.append("Thread:");
        sb.append(executionInfo.getThreadName());
        sb.append("(");
        sb.append(executionInfo.getThreadId());
        sb.append(")");
    };

    public static final BiConsumer<QueryExecutionInfo, StringBuilder> DEFAULT_ON_CONNECTION = (executionInfo, sb) -> {
        sb.append("Connection:");
        sb.append(executionInfo.getConnectionId());
    };

    public static final BiConsumer<QueryExecutionInfo, StringBuilder> DEFAULT_ON_SUCCESS = (executionInfo, sb) -> {
        sb.append("Success:");
        sb.append(executionInfo.isSuccess() ? "True" : "False");
    };

    public static final BiConsumer<QueryExecutionInfo, StringBuilder> DEFAULT_ON_TIME = (executionInfo, sb) -> {
        sb.append("Time:");
        sb.append(executionInfo.getExecuteDuration().toMillis());
    };

    public static final BiConsumer<QueryExecutionInfo, StringBuilder> DEFAULT_ON_TYPE = (executionInfo, sb) -> {
        sb.append("Type:");
        sb.append(executionInfo.getType() == ExecutionType.BATCH ? "Batch" : "Statement");
    };

    public static final BiConsumer<QueryExecutionInfo, StringBuilder> DEFAULT_ON_BATCH_SIZE = (executionInfo, sb) -> {
        sb.append("BatchSize:");
        sb.append(executionInfo.getBatchSize());
    };

    public static final BiConsumer<QueryExecutionInfo, StringBuilder> DEFAULT_ON_BINDINGS_SIZE = (executionInfo, sb) -> {
        sb.append("BindingsSize:");
        sb.append(executionInfo.getBindingsSize());
    };

    public static final BiConsumer<QueryExecutionInfo, StringBuilder> DEFAULT_ON_QUERY = (executionInfo, sb) -> {
        sb.append("Query:[");

        List<QueryInfo> queries = executionInfo.getQueries();
        if (!queries.isEmpty()) {
            String s = queries.stream()
                    .map(QueryInfo::getQuery)
                    .collect(joining("\",\"", "\"", "\""));
            sb.append(s);
        }

        sb.append("]");
    };

    public static final BiConsumer<BindingValue, StringBuilder> DEFAULT_ON_BINDING_VALUE = (bindingValue, sb) -> {
        if (bindingValue instanceof NullBindingValue) {
            Class<?> type = ((NullBindingValue) bindingValue).getType();
            sb.append("null(");
            sb.append(type.getSimpleName());
            sb.append(")");
        } else {
            sb.append(bindingValue.getValue());
        }
    };

    /**
     * generate comma separated values. "val1,val2,val3"
     */
    public static final BiConsumer<SortedSet<Binding>, StringBuilder> DEFAULT_ON_INDEX_BINDINGS = (indexBindings, sb) -> {
        String s = indexBindings.stream()
                .map(Binding::getBindingValue)
                .map(bindingValue -> {
                    StringBuilder sbuilder = new StringBuilder();
                    DEFAULT_ON_BINDING_VALUE.accept(bindingValue, sbuilder);
                    return sbuilder.toString();
                })
                .collect(joining(","));

        sb.append(s);
    };
    /**
     * Generate comma separated key-values pair string. "key1=val1,key2=val2,key3=val3"
     */
    public static final BiConsumer<SortedSet<Binding>, StringBuilder> DEFAULT_ON_IDENTIFIER_BINDINGS = (identifierBindings, sb) -> {
        String s = identifierBindings.stream()
                .map(binding -> {
                    StringBuilder sbuilder = new StringBuilder();
                    sbuilder.append(binding.getKey());
                    sbuilder.append("=");
                    DEFAULT_ON_BINDING_VALUE.accept(binding.getBindingValue(), sbuilder);
                    return sbuilder.toString();
                })
                .collect(joining(","));
        sb.append(s);
    };

    public static final BiConsumer<QueryExecutionInfo, StringBuilder> DEFAULT_ON_BINDINGS = (executionInfo, sb) -> {
        sb.append("Bindings:[");

        List<QueryInfo> queries = executionInfo.getQueries();
        if (!queries.isEmpty()) {
            String s = queries.stream()
                    .map(QueryInfo::getBindingsList)
                    .filter(bindings -> !bindings.isEmpty())
                    .map(bindings -> bindings.stream()
                            .map(binds -> {
                                StringBuilder sbForBindings = new StringBuilder();
                                SortedSet<Binding> indexBindings = binds.getIndexBindings();
                                if (!indexBindings.isEmpty()) {
                                    DEFAULT_ON_INDEX_BINDINGS.accept(indexBindings, sbForBindings);
                                }

                                SortedSet<Binding> identifierBindings = binds.getIdentifierBindings();
                                if (!identifierBindings.isEmpty()) {
                                    DEFAULT_ON_IDENTIFIER_BINDINGS.accept(identifierBindings, sbForBindings);
                                }
                                return sbForBindings.toString();
                            })
                            .collect(joining("),(", "(", ")")))
                    .collect(joining(","));
            sb.append(s);
        }

        sb.append("]");
    };

    public static final BiConsumer<QueryExecutionInfo, StringBuilder> DEFAULT_NEWLINE = (executionInfo, sb) -> {
        sb.append(System.lineSeparator());
    };

    private BiConsumer<QueryExecutionInfo, StringBuilder> onThread = DEFAULT_ON_THREAD;
    private BiConsumer<QueryExecutionInfo, StringBuilder> onConnection = DEFAULT_ON_CONNECTION;
    private BiConsumer<QueryExecutionInfo, StringBuilder> onSuccess = DEFAULT_ON_SUCCESS;
    private BiConsumer<QueryExecutionInfo, StringBuilder> onTime = DEFAULT_ON_TIME;
    private BiConsumer<QueryExecutionInfo, StringBuilder> onType = DEFAULT_ON_TYPE;
    private BiConsumer<QueryExecutionInfo, StringBuilder> onBatchSize = DEFAULT_ON_BATCH_SIZE;
    private BiConsumer<QueryExecutionInfo, StringBuilder> onBindingsSize = DEFAULT_ON_BINDINGS_SIZE;
    private BiConsumer<QueryExecutionInfo, StringBuilder> onQuery = DEFAULT_ON_QUERY;
    private BiConsumer<QueryExecutionInfo, StringBuilder> onBindings = DEFAULT_ON_BINDINGS;
    private BiConsumer<QueryExecutionInfo, StringBuilder> newLine = DEFAULT_NEWLINE;
    private String delimiter = DEFAULT_DELIMITER;

    private List<BiConsumer<QueryExecutionInfo, StringBuilder>> consumers = new ArrayList<>();


    public static ExecutionInfoFormatter showAll() {
        ExecutionInfoFormatter formatter = new ExecutionInfoFormatter();
        formatter.addConsumer(formatter.onThread);
        formatter.addConsumer(formatter.onConnection);
        formatter.addConsumer(formatter.onSuccess);
        formatter.addConsumer(formatter.onTime);
        formatter.addConsumer(formatter.onType);
        formatter.addConsumer(formatter.onBatchSize);
        formatter.addConsumer(formatter.onBindingsSize);
        formatter.addConsumer(formatter.onQuery);
        formatter.addConsumer(formatter.onBindings);
        return formatter;
    }

    public ExecutionInfoFormatter addConsumer(BiConsumer<QueryExecutionInfo, StringBuilder> consumer) {
        this.consumers.add(consumer);
        return this;
    }

    public String format(QueryExecutionInfo executionInfo) {

        StringBuilder sb = new StringBuilder();

        consumers.forEach(consumer -> {
            consumer.accept(executionInfo, sb);

            // if it is for new line, skip adding delimiter
            if (consumer != this.newLine) {
                sb.append(this.delimiter);
            }
        });

        chompIfEndWith(sb, this.delimiter);

        return sb.toString();

    }

    @Override
    public String apply(QueryExecutionInfo executionInfo) {
        return format(executionInfo);
    }

    protected void chompIfEndWith(StringBuilder sb, String s) {
        if (sb.length() < s.length()) {
            return;
        }
        final int startIndex = sb.length() - s.length();
        if (sb.substring(startIndex, sb.length()).equals(s)) {
            sb.delete(startIndex, sb.length());
        }
    }

    public ExecutionInfoFormatter delimiter(String delimiter) {
        this.delimiter = delimiter;
        return this;
    }


    public ExecutionInfoFormatter showThread() {
        this.consumers.add(this.onThread);
        return this;
    }

    public ExecutionInfoFormatter showThread(BiConsumer<QueryExecutionInfo, StringBuilder> consumer) {
        this.onThread = consumer;
        return showThread();
    }

    public ExecutionInfoFormatter showConnection() {
        this.consumers.add(this.onConnection);
        return this;
    }

    public ExecutionInfoFormatter showConnection(BiConsumer<QueryExecutionInfo, StringBuilder> consumer) {
        this.onConnection = consumer;
        return showConnection();
    }

    public ExecutionInfoFormatter showSuccess() {
        this.consumers.add(this.onSuccess);
        return this;
    }

    public ExecutionInfoFormatter showSuccess(BiConsumer<QueryExecutionInfo, StringBuilder> consumer) {
        this.onSuccess = consumer;
        return showSuccess();
    }

    public ExecutionInfoFormatter showTime() {
        this.consumers.add(this.onTime);
        return this;
    }

    public ExecutionInfoFormatter showTime(BiConsumer<QueryExecutionInfo, StringBuilder> consumer) {
        this.onTime = consumer;
        return showTime();
    }

    public ExecutionInfoFormatter showType() {
        this.consumers.add(this.onType);
        return this;
    }

    public ExecutionInfoFormatter showType(BiConsumer<QueryExecutionInfo, StringBuilder> consumer) {
        this.onType = consumer;
        return showType();
    }


    public ExecutionInfoFormatter showBatchSize() {
        this.consumers.add(this.onBatchSize);
        return this;
    }

    public ExecutionInfoFormatter showBatchSize(BiConsumer<QueryExecutionInfo, StringBuilder> consumer) {
        this.onBatchSize = consumer;
        return showBatchSize();
    }

    public ExecutionInfoFormatter showBindingsSize() {
        this.consumers.add(this.onBindingsSize);
        return this;
    }

    public ExecutionInfoFormatter showBindingsSize(BiConsumer<QueryExecutionInfo, StringBuilder> consumer) {
        this.onBindingsSize = consumer;
        return showBindingsSize();
    }


    public ExecutionInfoFormatter showQuery() {
        this.consumers.add(this.onQuery);
        return this;
    }

    public ExecutionInfoFormatter showQuery(BiConsumer<QueryExecutionInfo, StringBuilder> consumer) {
        this.onQuery = consumer;
        return showQuery();
    }


    public ExecutionInfoFormatter showBindings() {
        this.consumers.add(this.onBindings);
        return this;
    }

    public ExecutionInfoFormatter showBindings(BiConsumer<QueryExecutionInfo, StringBuilder> consumer) {
        this.onBindings = consumer;
        return showBindings();
    }

    /**
     * Change the line
     */
    public ExecutionInfoFormatter newLine() {
        this.consumers.add(this.newLine);
        return this;
    }

}
