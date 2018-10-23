package net.ttddyy.dsproxy.r2dbc;

import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Statement;

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
    public Connection createConnection(Connection connection, String connectionId) {
        return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[] { Connection.class },
                new ConnectionInvocationHandler(connection, connectionId, this.proxyConfig));
    }

    @Override
    public Batch<?> createBatch(Batch<?> batch, String connectionId) {
        return (Batch<?>) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[] { Batch.class },
                new BatchInvocationHandler(batch, connectionId, this.proxyConfig));
    }

    @Override
    public Statement<?> createStatement(Statement<?> statement, String query, String connectionId) {
        return (Statement<?>) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[] { Statement.class },
                new StatementInvocationHandler(statement, query, connectionId, this.proxyConfig));
    }

    public static class ConnectionInvocationHandler implements InvocationHandler {

        private ReactiveConnectionCallback delegate;

        public ConnectionInvocationHandler(Connection connection, String connectionId, ProxyConfig proxyConfig) {
            this.delegate = new ReactiveConnectionCallback(connection, connectionId, proxyConfig);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return delegate.invoke(proxy, method, args);
        }
    }

    public static class BatchInvocationHandler implements InvocationHandler {

        private ReactiveBatchCallback delegate;

        public BatchInvocationHandler(Batch<?> batch, String connectionId, ProxyConfig proxyConfig) {
            this.delegate = new ReactiveBatchCallback(batch, connectionId, proxyConfig);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return delegate.invoke(proxy, method, args);
        }
    }

    public static class StatementInvocationHandler implements InvocationHandler {

        private ReactiveStatementCallback delegate;

        public StatementInvocationHandler(Statement<?> statement, String query, String connectionId, ProxyConfig proxyConfig) {
            this.delegate = new ReactiveStatementCallback(statement, query, connectionId, proxyConfig);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return delegate.invoke(proxy, method, args);
        }
    }

}
