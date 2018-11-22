package net.ttddyy.dsproxy.r2dbc.proxy;

import net.ttddyy.dsproxy.r2dbc.proxy.ProxyFactory;
import net.ttddyy.dsproxy.r2dbc.proxy.ReactiveBatchCallback;
import net.ttddyy.dsproxy.r2dbc.proxy.ReactiveConnectionCallback;
import net.ttddyy.dsproxy.r2dbc.proxy.ReactiveConnectionFactoryCallback;
import net.ttddyy.dsproxy.r2dbc.proxy.ReactiveStatementCallback;

/**
 * Provide a method to unwrap the original object from proxy object.
 *
 * <p>Proxy object created by {@link ProxyFactory} implements this interface.
 *
 * @author Tadaya Tsuyukubo
 * @see ProxyFactory
 * @see ReactiveConnectionFactoryCallback
 * @see ReactiveConnectionCallback
 * @see ReactiveBatchCallback
 * @see ReactiveStatementCallback
 */
public interface ProxyObject {

    /**
     * Method to return the source object (ConnectionFactory, Connection, Batch, Statement).
     *
     * @return source object
     */
    Object getTarget();

}
