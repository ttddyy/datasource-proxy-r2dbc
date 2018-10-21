package net.ttddyy.dsproxy.r2dbc.core;

/**
 *
 * @author Tadaya Tsuyukubo
 */
public interface ProxyDataSourceListener {

    default void beforeQuery(ExecutionInfo execInfo) {
    }

    default void afterQuery(ExecutionInfo execInfo) {
    }

}
