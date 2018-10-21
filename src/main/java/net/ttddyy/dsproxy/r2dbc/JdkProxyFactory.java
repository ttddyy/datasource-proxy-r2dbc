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

    public void setProxyConfig(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    public Connection createConnection(Connection connection) {
        return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[] { Connection.class },
                new ConnectionInvocationHandler(connection, this.proxyConfig));
    }

    public Batch<?> createBatch(Batch<?> batch) {
        return (Batch<?>) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[] { Batch.class },
                new BatchInvocationHandler(batch, this.proxyConfig));
    }

    public Statement<?> createStatement(Statement<?> statement, String query) {
        return (Statement<?>) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[] { Statement.class },
                new StatementInvocationHandler(statement, query, this.proxyConfig));
    }

    public static class ConnectionInvocationHandler implements InvocationHandler {

        private ReactiveConnectionCallback delegate;

        public ConnectionInvocationHandler(Connection connection, ProxyConfig proxyConfig) {
            this.delegate = new ReactiveConnectionCallback(connection, proxyConfig);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return delegate.invoke(proxy, method, args);
        }
    }

    public static class BatchInvocationHandler implements InvocationHandler {

        private ReactiveBatchCallback delegate;

        public BatchInvocationHandler(Batch<?> batch, ProxyConfig proxyConfig) {
            this.delegate = new ReactiveBatchCallback(batch, proxyConfig);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return delegate.invoke(proxy, method, args);
        }
    }

    public static class StatementInvocationHandler implements InvocationHandler {

        private ReactiveStatementCallback delegate;

        public StatementInvocationHandler(Statement<?> statement, String query, ProxyConfig proxyConfig) {
            this.delegate = new ReactiveStatementCallback(statement, query, proxyConfig);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return delegate.invoke(proxy, method, args);
        }
    }

}
