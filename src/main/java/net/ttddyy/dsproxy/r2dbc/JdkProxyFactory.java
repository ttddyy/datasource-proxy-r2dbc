package net.ttddyy.dsproxy.r2dbc;

import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Statement;
import net.ttddyy.dsproxy.r2dbc.core.ConnectionHolder;
import net.ttddyy.dsproxy.r2dbc.core.ConnectionInfo;
import net.ttddyy.dsproxy.r2dbc.core.ProxyObject;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * ProxyFactory using JDK dynamic proxy.
 *
 * @author Tadaya Tsuyukubo
 */
public class JdkProxyFactory implements ProxyFactory {

    private ProxyConfig proxyConfig;

    @Override
    public void setProxyConfig(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    @Override
    public ConnectionFactory createConnectionFactory(ConnectionFactory connectionFactory) {
        return (ConnectionFactory) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class[]{ConnectionFactory.class, ProxyObject.class},
                new ConnectionFactoryInvocationHandler(connectionFactory, this.proxyConfig));
    }

    @Override
    public Connection createConnection(Connection connection, ConnectionInfo connectionInfo) {
        return (Connection) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class[]{Connection.class, ProxyObject.class, ConnectionHolder.class},
                new ConnectionInvocationHandler(connection, connectionInfo, this.proxyConfig));
    }

    @Override
    public Batch<?> createBatch(Batch<?> batch, ConnectionInfo connectionInfo) {
        return (Batch<?>) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class[]{Batch.class, ProxyObject.class, ConnectionHolder.class},
                new BatchInvocationHandler(batch, connectionInfo, this.proxyConfig));
    }

    @Override
    public Statement<?> createStatement(Statement<?> statement, String query, ConnectionInfo connectionInfo) {
        return (Statement<?>) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class[]{Statement.class, ProxyObject.class, ConnectionHolder.class},
                new StatementInvocationHandler(statement, query, connectionInfo, this.proxyConfig));
    }

    public static class ConnectionFactoryInvocationHandler implements InvocationHandler {

        private ReactiveConnectionFactoryCallback delegate;

        public ConnectionFactoryInvocationHandler(ConnectionFactory connectionFactory, ProxyConfig proxyConfig) {
            this.delegate = new ReactiveConnectionFactoryCallback(connectionFactory, proxyConfig);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return delegate.invoke(proxy, method, args);
        }
    }

    public static class ConnectionInvocationHandler implements InvocationHandler {

        private ReactiveConnectionCallback delegate;

        public ConnectionInvocationHandler(Connection connection, ConnectionInfo connectionInfo, ProxyConfig proxyConfig) {
            this.delegate = new ReactiveConnectionCallback(connection, connectionInfo, proxyConfig);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return delegate.invoke(proxy, method, args);
        }
    }

    public static class BatchInvocationHandler implements InvocationHandler {

        private ReactiveBatchCallback delegate;

        public BatchInvocationHandler(Batch<?> batch, ConnectionInfo connectionInfo, ProxyConfig proxyConfig) {
            this.delegate = new ReactiveBatchCallback(batch, connectionInfo, proxyConfig);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return delegate.invoke(proxy, method, args);
        }
    }

    public static class StatementInvocationHandler implements InvocationHandler {

        private ReactiveStatementCallback delegate;

        public StatementInvocationHandler(Statement<?> statement, String query, ConnectionInfo connectionInfo, ProxyConfig proxyConfig) {
            this.delegate = new ReactiveStatementCallback(statement, query, connectionInfo, proxyConfig);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return delegate.invoke(proxy, method, args);
        }
    }

}
