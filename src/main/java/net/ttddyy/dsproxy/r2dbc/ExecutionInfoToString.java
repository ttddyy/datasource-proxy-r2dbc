package net.ttddyy.dsproxy.r2dbc;

import net.ttddyy.dsproxy.r2dbc.core.Binding;
import net.ttddyy.dsproxy.r2dbc.core.ExecutionInfo;
import net.ttddyy.dsproxy.r2dbc.core.ExecutionType;
import net.ttddyy.dsproxy.r2dbc.core.QueryInfo;

import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;

/**
 * @author Tadaya Tsuyukubo
 */
public class ExecutionInfoToString implements Function<ExecutionInfo, String> {

    @Override
    public String apply(ExecutionInfo executionInfo) {
        StringBuilder sb = new StringBuilder();

        // Thread: name(id)
        sb.append("Thread:");
        //        sb.append(executionInfo.getThreadName());
        sb.append("(");
        //        sb.append(executionInfo.getThreadId());
        sb.append(")");

        sb.append(" Success:");
        sb.append(executionInfo.isSuccess() ? "True" : "False");

        sb.append(" Time:");
        sb.append(executionInfo.getElapsedTime());

        sb.append(" Type:");
        if (executionInfo.getType() == ExecutionType.BATCH) {
            sb.append("Batch");
        } else if (executionInfo.getType() == ExecutionType.STATEMENT) {
            sb.append("Statement");
        } else {
            sb.append("Unknown");
        }

        sb.append(" BatchSize:");
        sb.append(executionInfo.getBatchSize());

        sb.append(" StatementSize:");
        sb.append(executionInfo.getStatementSize());

        sb.append(" Query:[");
        String queries = executionInfo.getQueries().stream()
                .map(QueryInfo::getQuery)
                .collect(joining("\",\"", "\"", "\""));
        sb.append(queries);
        sb.append("]");

        sb.append(",");

        // TODO: cleanup
        sb.append(" Bindings:[");
        String params = executionInfo.getQueries().stream()
                .map(QueryInfo::getBindingsList)
                .filter(list -> !list.isEmpty())
                .map(bindingsEntry ->
                        bindingsEntry.stream()
                                .map(bindings -> {
                                    if (!bindings.getIndexBindings().isEmpty()) {
                                        return bindings.getIndexBindings().stream()
                                                .map(Binding::getValue)
                                                .map(String::valueOf)
                                                .collect(joining(","));
                                    } else {
                                        return bindings.getIdentifierBindings().stream()
                                                .map(binding -> {
                                                    String key = String.valueOf(binding.getKey());
                                                    String value = String.valueOf(binding.getValue());
                                                    return key + "=" + value;
                                                })
                                                .collect(joining(","));
                                    }
                                })
                                .collect(joining("),(", "(", ")")))
                .collect(joining(","));
        sb.append(params);
        sb.append("]");

        return sb.toString();
    }

    public ExecutionInfoToString onQueries(BiConsumer<ExecutionInfo, StringBuilder> consumer) {
        return this;
    }

}
