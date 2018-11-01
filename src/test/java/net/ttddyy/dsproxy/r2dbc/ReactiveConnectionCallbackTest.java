package net.ttddyy.dsproxy.r2dbc;

import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Statement;
import net.ttddyy.dsproxy.r2dbc.core.ConnectionInfo;
import net.ttddyy.dsproxy.r2dbc.core.LastExecutionAwareListener;
import net.ttddyy.dsproxy.r2dbc.core.MethodExecutionInfo;
import net.ttddyy.dsproxy.r2dbc.core.ProxyObject;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Tadaya Tsuyukubo
 */
public class ReactiveConnectionCallbackTest {

    private static Method CREATE_BATCH_METHOD = ReflectionUtils.findMethod(Connection.class, "createBatch");
    private static Method CREATE_STATEMENT_METHOD = ReflectionUtils.findMethod(Connection.class, "createStatement", String.class);
    private static Method BEGIN_TRANSACTION_METHOD = ReflectionUtils.findMethod(Connection.class, "beginTransaction");
    private static Method COMMIT_TRANSACTION_METHOD = ReflectionUtils.findMethod(Connection.class, "commitTransaction");
    private static Method ROLLBACK_TRANSACTION_METHOD = ReflectionUtils.findMethod(Connection.class, "rollbackTransaction");
    private static Method CLOSE_METHOD = ReflectionUtils.findMethod(Connection.class, "close");
    private static Method GET_TARGET_METHOD = ReflectionUtils.findMethod(ProxyObject.class, "getTarget");

    @Test
    void createBatch() throws Throwable {
        LastExecutionAwareListener listener = new LastExecutionAwareListener();

        Connection connection = mock(Connection.class);
        ConnectionInfo connectionInfo = new ConnectionInfo();

        ProxyFactory proxyFactory = mock(ProxyFactory.class);

        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.addListener(listener);
        proxyConfig.setProxyFactory(proxyFactory);

        Batch originalBatch = mock(Batch.class);
        Batch resultBatch = mock(Batch.class);
        when(connection.createBatch()).thenReturn(originalBatch);

        when(proxyFactory.createBatch(originalBatch, connectionInfo)).thenReturn(resultBatch);

        ReactiveConnectionCallback callback = new ReactiveConnectionCallback(connection, connectionInfo, proxyConfig);

        Object result = callback.invoke(null, CREATE_BATCH_METHOD, null);

        assertSame(resultBatch, result);

        MethodExecutionInfo executionInfo = listener.getAfterMethodExecutionInfo();
        assertEquals(originalBatch, executionInfo.getResult());
    }

    @Test
    void createStatement() throws Throwable {
        LastExecutionAwareListener listener = new LastExecutionAwareListener();
        ProxyFactory proxyFactory = mock(ProxyFactory.class);

        Connection connection = mock(Connection.class);
        ConnectionInfo connectionInfo = new ConnectionInfo();

        String query = "MY-QUERY";

        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.addListener(listener);
        proxyConfig.setProxyFactory(proxyFactory);

        Statement originalStatement = mock(Statement.class);
        Statement resultStatement = mock(Statement.class);
        when(connection.createStatement(query)).thenReturn(originalStatement);

        when(proxyFactory.createStatement(originalStatement, query, connectionInfo)).thenReturn(resultStatement);

        ReactiveConnectionCallback callback = new ReactiveConnectionCallback(connection, connectionInfo, proxyConfig);

        Object result = callback.invoke(null, CREATE_STATEMENT_METHOD, new Object[]{query});

        assertSame(resultStatement, result);

        MethodExecutionInfo executionInfo = listener.getAfterMethodExecutionInfo();
        assertEquals(originalStatement, executionInfo.getResult());
    }

    @Test
    void beginTransaction() throws Throwable {
        LastExecutionAwareListener listener = new LastExecutionAwareListener();

        Connection connection = mock(Connection.class);
        ConnectionInfo connectionInfo = new ConnectionInfo();
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.addListener(listener);

        when(connection.beginTransaction()).thenReturn(Mono.empty());

        ReactiveConnectionCallback callback = new ReactiveConnectionCallback(connection, connectionInfo, proxyConfig);

        Object result = callback.invoke(null, BEGIN_TRANSACTION_METHOD, null);

        StepVerifier.create((Publisher<Void>) result)
                .expectSubscription()
                // since it is a Publisher<Void>, no steps for assertNext
                .verifyComplete();

        MethodExecutionInfo executionInfo = listener.getAfterMethodExecutionInfo();
        assertSame(connectionInfo, executionInfo.getConnectionInfo());

        assertEquals(1, connectionInfo.getTransactionCount());
    }

    @Test
    void commitTransaction() throws Throwable {
        LastExecutionAwareListener listener = new LastExecutionAwareListener();

        Connection connection = mock(Connection.class);
        ConnectionInfo connectionInfo = new ConnectionInfo();
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.addListener(listener);

        when(connection.commitTransaction()).thenReturn(Mono.empty());

        ReactiveConnectionCallback callback = new ReactiveConnectionCallback(connection, connectionInfo, proxyConfig);

        Object result = callback.invoke(null, COMMIT_TRANSACTION_METHOD, null);

        StepVerifier.create((Publisher<Void>) result)
                .expectSubscription()
                // since it is a Publisher<Void>, no steps for assertNext
                .verifyComplete();

        MethodExecutionInfo executionInfo = listener.getAfterMethodExecutionInfo();
        assertSame(connectionInfo, executionInfo.getConnectionInfo());

        assertEquals(1, connectionInfo.getCommitCount());
    }

    @Test
    void rollbackTransaction() throws Throwable {
        LastExecutionAwareListener listener = new LastExecutionAwareListener();

        Connection connection = mock(Connection.class);
        ConnectionInfo connectionInfo = new ConnectionInfo();
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.addListener(listener);

        when(connection.rollbackTransaction()).thenReturn(Mono.empty());

        ReactiveConnectionCallback callback = new ReactiveConnectionCallback(connection, connectionInfo, proxyConfig);

        Object result = callback.invoke(null, ROLLBACK_TRANSACTION_METHOD, null);

        StepVerifier.create((Publisher<Void>) result)
                .expectSubscription()
                // since it is a Publisher<Void>, no steps for assertNext
                .verifyComplete();

        MethodExecutionInfo executionInfo = listener.getAfterMethodExecutionInfo();
        assertSame(connectionInfo, executionInfo.getConnectionInfo());

        assertEquals(1, connectionInfo.getRollbackCount());
    }

    @Test
    void close() throws Throwable {
        LastExecutionAwareListener listener = new LastExecutionAwareListener();

        Connection connection = mock(Connection.class);
        ConnectionInfo connectionInfo = new ConnectionInfo();
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.addListener(listener);

        when(connection.close()).thenReturn(Mono.empty());

        ReactiveConnectionCallback callback = new ReactiveConnectionCallback(connection, connectionInfo, proxyConfig);

        Object result = callback.invoke(null, CLOSE_METHOD, null);

        StepVerifier.create((Publisher<Void>) result)
                .expectSubscription()
                // since it is a Publisher<Void>, no steps for assertNext
                .verifyComplete();

        MethodExecutionInfo executionInfo = listener.getAfterMethodExecutionInfo();
        assertSame(connectionInfo, executionInfo.getConnectionInfo());

        assertTrue(connectionInfo.isClosed());
    }

    @Test
    void getTarget() throws Throwable {
        Connection connection = mock(Connection.class);
        ConnectionInfo connectionInfo = new ConnectionInfo();
        ProxyConfig proxyConfig = new ProxyConfig();

        ReactiveConnectionCallback callback = new ReactiveConnectionCallback(connection, connectionInfo, proxyConfig);

        Object result = callback.invoke(null, GET_TARGET_METHOD, null);
        assertSame(connection, result);
    }

}
