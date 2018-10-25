package net.ttddyy.dsproxy.r2dbc.core;

import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Statement;

/**
 * Listener interface that is called by proxy on {@link io.r2dbc.spi.Connection},
 * {@link io.r2dbc.spi.Statement}, or {@link io.r2dbc.spi.Batch}.
 *
 * @author Tadaya Tsuyukubo
 */
public interface ProxyExecutionListener {

    /**
     * Called before every invocation of methods.
     *
     * @param executionInfo method execution context
     */
    default void beforeMethod(MethodExecutionInfo executionInfo) {
    }

    /**
     * Called after every invocation of methods.
     *
     * @param executionInfo method execution context
     */
    default void afterMethod(MethodExecutionInfo executionInfo) {
    }

    /**
     * Called before execution of query.
     * <p>
     * Query execution is {@link Batch#execute()} or {@link Statement#execute()}.
     *
     * @param execInfo query execution context
     */
    default void beforeQuery(QueryExecutionInfo execInfo) {
    }

    /**
     * Called after execution of query.
     * <p>
     * Query execution is {@link Batch#execute()} or {@link Statement#execute()}.
     *
     * @param execInfo query execution context
     */
    default void afterQuery(QueryExecutionInfo execInfo) {
    }

}
