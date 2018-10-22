package net.ttddyy.dsproxy.r2dbc.core;

/**
 *
 * @author Tadaya Tsuyukubo
 */
public interface ProxyDataSourceListener {

    // TODO: beforeMethod, afterMethod

    default void beforeQuery(ExecutionInfo execInfo) {
    }

    default void afterQuery(ExecutionInfo execInfo) {
    }

}
