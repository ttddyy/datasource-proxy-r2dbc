package net.ttddyy.dsproxy.r2dbc.core;

/**
 * Provide a method to unwrap the original object from proxy object.
 *
 * <p>Proxy object created by {@link net.ttddyy.dsproxy.r2dbc.ProxyFactory} implements this interface.
 *
 * @author Tadaya Tsuyukubo
 * @see net.ttddyy.dsproxy.r2dbc.ProxyFactory
 * @see net.ttddyy.dsproxy.r2dbc.ReactiveConnectionFactoryCallback
 * @see net.ttddyy.dsproxy.r2dbc.ReactiveConnectionCallback
 * @see net.ttddyy.dsproxy.r2dbc.ReactiveBatchCallback
 * @see net.ttddyy.dsproxy.r2dbc.ReactiveStatementCallback
 */
public interface ProxyObject {

    /**
     * Method to return the source object (ConnectionFactory, Connection, Batch, Statement).
     *
     * @return source object
     */
    Object getTarget();

}
