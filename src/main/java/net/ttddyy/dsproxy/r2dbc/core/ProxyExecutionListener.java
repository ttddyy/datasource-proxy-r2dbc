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
     * Called before and after every invocation of methods.
     *
     * Default implementation of this method dispatches to {@link #beforeMethod(MethodExecutionInfo)} and
     * {@link #afterMethod(MethodExecutionInfo)}. Thus overriding this method will stop calling them
     * unless subclass calls {@code super.onMethodExecution(executionInfo);}.
     *
     * @param executionInfo query execution context
     */
    default void onMethodExecution(MethodExecutionInfo executionInfo) {
        ProxyEventType eventType = executionInfo.getProxyEventType();
        if (eventType == ProxyEventType.BEFORE_METHOD) {
            beforeMethod(executionInfo);
        } else if (eventType == ProxyEventType.AFTER_METHOD) {
            afterMethod(executionInfo);
        }
    }

    /**
     * Called before and after execution of query.
     *
     * Default implementation of this method dispatches to {@link #beforeQuery(QueryExecutionInfo)} and
     * {@link #afterQuery(QueryExecutionInfo)}. Thus overriding this method will stop calling them
     * unless subclass calls {@code super.onQueryExecution(executionInfo);}.
     *
     * @param executionInfo query execution context
     */
    default void onQueryExecution(QueryExecutionInfo executionInfo) {
        ProxyEventType eventType = executionInfo.getProxyEventType();
        if (eventType == ProxyEventType.BEFORE_QUERY) {
            beforeQuery(executionInfo);
        } else if (eventType == ProxyEventType.AFTER_QUERY) {
            afterQuery(executionInfo);
        }
    }

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
     *
     * Query execution is {@link Batch#execute()} or {@link Statement#execute()}.
     *
     * Note: this callback is called when the publisher, result of the {@code execute()}, is being
     * subscribed. Not at the time of {@code execute()} is called,
     *
     * @param execInfo query execution context
     */
    default void beforeQuery(QueryExecutionInfo execInfo) {
    }

    /**
     * Called after execution of query.
     *
     * Query execution is {@link Batch#execute()} or {@link Statement#execute()}.
     *
     * Note: this callback is called when the publisher, result of the {@code execute()}, is being
     * subscribed. Not at the time of {@code execute()} is called,
     *
     * @param execInfo query execution context
     */
    default void afterQuery(QueryExecutionInfo execInfo) {
    }

    /**
     * Called on processing each query {@link io.r2dbc.spi.Result}.
     *
     * While subscribing query results, this callback is called per result.
     *
     * @param execInfo query execution context
     */
    default void eachQueryResult(QueryExecutionInfo execInfo) {
    }

}
