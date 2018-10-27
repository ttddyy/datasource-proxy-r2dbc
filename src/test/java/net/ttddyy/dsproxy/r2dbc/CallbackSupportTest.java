package net.ttddyy.dsproxy.r2dbc;

import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import net.ttddyy.dsproxy.r2dbc.core.ConnectionIdManager;
import net.ttddyy.dsproxy.r2dbc.core.LastExecutionAwareListener;
import net.ttddyy.dsproxy.r2dbc.core.MethodExecutionInfo;
import net.ttddyy.dsproxy.r2dbc.core.ProxyEventType;
import net.ttddyy.dsproxy.r2dbc.core.QueryExecutionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Publisher;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;
import reactor.util.function.Tuple2;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Tadaya Tsuyukubo
 */
@ExtendWith(MockitoExtension.class)
public class CallbackSupportTest {

    private CallbackSupport callbackSupport;

    @Mock
    private ProxyConfig proxyConfig;

    @BeforeEach
    void setUp() {
        this.callbackSupport = new CallbackSupport(this.proxyConfig) {
        };

        Clock clock = Clock.fixed(Instant.ofEpochSecond(100), ZoneId.systemDefault());
        this.callbackSupport.setClock(clock);
    }

    // TODO: write test for proceedExecution() for ConnectionFactory#create()

    @Test
    void interceptQueryExecution() {

        LastExecutionAwareListener listener = new LastExecutionAwareListener();
        QueryExecutionInfo executionInfo = new QueryExecutionInfo();

        // produce single result in order to trigger doOnNext
        Result mockResult = mock(Result.class);
        Mono<Result> publisher = Mono.just(mockResult);

        Flux<? extends Result> result = this.callbackSupport.interceptQueryExecution(publisher, listener, executionInfo);

        // verifies result flux
        StepVerifier.create(result)
                .expectSubscription()
                .consumeNextWith(c -> {
                    // in middle of chain, beforeQuery must be called
                    assertSame(mockResult, c);
                    assertEquals(ProxyEventType.BEFORE_QUERY, executionInfo.getProxyEventType());
                    assertSame(executionInfo, listener.getBeforeQueryExecutionInfo());
                })
                .expectComplete()
                .verify();


        assertNull(listener.getBeforeMethodExecutionInfo());
        assertNull(listener.getAfterMethodExecutionInfo());
        assertSame(executionInfo, listener.getBeforeQueryExecutionInfo());
        assertSame(executionInfo, listener.getAfterQueryExecutionInfo());

        assertEquals(ProxyEventType.AFTER_QUERY, executionInfo.getProxyEventType());

        String threadName = Thread.currentThread().getName();
        long threadId = Thread.currentThread().getId();
        assertEquals(threadName, executionInfo.getThreadName());
        assertEquals(threadId, executionInfo.getThreadId());

        // since it uses fixed clock that returns same time, duration is 0
        assertEquals(Duration.ZERO, executionInfo.getExecuteDuration());

        // verify success
        assertTrue(executionInfo.isSuccess());
        assertNull(executionInfo.getThrowable());

    }

    @Test
    void interceptQueryExecutionWithFailure() {

        LastExecutionAwareListener listener = new LastExecutionAwareListener();
        QueryExecutionInfo executionInfo = new QueryExecutionInfo();

        // publisher that throws exception
        RuntimeException exception = new RuntimeException();
        Publisher<Result> publisher = TestPublisher.<Result>create().error(exception);

        Flux<? extends Result> result = this.callbackSupport.interceptQueryExecution(publisher, listener, executionInfo);

        // verifies result flux
        StepVerifier.create(result)
                .expectSubscription()
                .verifyError(RuntimeException.class);


        assertNull(listener.getBeforeMethodExecutionInfo());
        assertNull(listener.getAfterMethodExecutionInfo());
        assertSame(executionInfo, listener.getBeforeQueryExecutionInfo());
        assertSame(executionInfo, listener.getAfterQueryExecutionInfo());

        assertEquals(ProxyEventType.AFTER_QUERY, executionInfo.getProxyEventType());

        String threadName = Thread.currentThread().getName();
        long threadId = Thread.currentThread().getId();
        assertEquals(threadName, executionInfo.getThreadName());
        assertEquals(threadId, executionInfo.getThreadId());

        // since it uses fixed clock that returns same time, duration is 0
        assertEquals(Duration.ZERO, executionInfo.getExecuteDuration());

        // verify failure
        assertFalse(executionInfo.isSuccess());
        assertSame(exception, executionInfo.getThrowable());
    }

    @SuppressWarnings("unchecked")
    @Test
    void proceedExecutionWithPublisher() throws Throwable {

        // target method returns Producer
        Method executeMethod = ReflectionUtils.findMethod(Batch.class, "execute");
        Batch target = mock(Batch.class);
        Object[] args = new Object[]{};
        LastExecutionAwareListener listener = new LastExecutionAwareListener();
        String connectionId = "conn-id";

        // produce single result in order to trigger StepVerifier#consumeNextWith.
        Result mockResult = mock(Result.class);
        Mono<Result> publisher = Mono.just(mockResult);

        when(target.execute()).thenReturn(publisher);

        Object result = this.callbackSupport.proceedExecution(executeMethod, target, args, listener, connectionId);

        // verify method on target is invoked
        verify(target).execute();

        StepVerifier.create((Publisher<Result>) result)
                .expectSubscription()
                .consumeNextWith(c -> {
                    // in middle of chain, beforeMethod must be called
                    assertSame(mockResult, c);

                    MethodExecutionInfo beforeMethod = listener.getBeforeMethodExecutionInfo();
                    assertNotNull(beforeMethod);
                    assertNull(listener.getAfterMethodExecutionInfo());

                    assertEquals(ProxyEventType.BEFORE_METHOD, beforeMethod.getProxyEventType());
                })
                .expectComplete()
                .verify();


        MethodExecutionInfo beforeMethodExecution = listener.getBeforeMethodExecutionInfo();
        MethodExecutionInfo afterMethodExecution = listener.getAfterMethodExecutionInfo();
        assertSame(beforeMethodExecution, afterMethodExecution);

        assertNull(listener.getBeforeQueryExecutionInfo());
        assertNull(listener.getAfterQueryExecutionInfo());

        assertEquals(target, afterMethodExecution.getTarget());
        assertEquals(mockResult, afterMethodExecution.getResult());
        assertEquals(executeMethod, afterMethodExecution.getMethod());
        assertEquals(args, afterMethodExecution.getMethodArgs());
        assertEquals(connectionId, afterMethodExecution.getConnectionId());

        String threadName = Thread.currentThread().getName();
        long threadId = Thread.currentThread().getId();
        assertEquals(threadName, afterMethodExecution.getThreadName());
        assertEquals(threadId, afterMethodExecution.getThreadId());

        // since it uses fixed clock that returns same time, duration is 0
        assertEquals(Duration.ZERO, afterMethodExecution.getExecuteDuration());

        assertEquals(ProxyEventType.AFTER_METHOD, afterMethodExecution.getProxyEventType());

        assertNull(afterMethodExecution.getThrown());
    }

    @SuppressWarnings("unchecked")
    @Test
    void proceedExecutionWithPublisherThrowsException() throws Throwable {

        // target method returns Producer
        Method executeMethod = ReflectionUtils.findMethod(Batch.class, "execute");
        Batch target = mock(Batch.class);
        Object[] args = new Object[]{};
        LastExecutionAwareListener listener = new LastExecutionAwareListener();
        String connectionId = "conn-id";

        // publisher that throws exception
        RuntimeException exception = new RuntimeException();
        Publisher<Result> publisher = TestPublisher.<Result>create().error(exception);

        when(target.execute()).thenReturn(publisher);

        Object result = this.callbackSupport.proceedExecution(executeMethod, target, args, listener, connectionId);

        // verify method on target is invoked
        verify(target).execute();

        StepVerifier.create((Publisher<Result>) result)
                .expectSubscription()
                .expectError(RuntimeException.class)
                .verify();


        MethodExecutionInfo beforeMethodExecution = listener.getBeforeMethodExecutionInfo();
        MethodExecutionInfo afterMethodExecution = listener.getAfterMethodExecutionInfo();
        assertSame(beforeMethodExecution, afterMethodExecution);

        assertNull(listener.getBeforeQueryExecutionInfo());
        assertNull(listener.getAfterQueryExecutionInfo());

        assertNull(afterMethodExecution.getResult());

        assertEquals(target, afterMethodExecution.getTarget());
        assertEquals(executeMethod, afterMethodExecution.getMethod());
        assertEquals(args, afterMethodExecution.getMethodArgs());
        assertEquals(connectionId, afterMethodExecution.getConnectionId());

        String threadName = Thread.currentThread().getName();
        long threadId = Thread.currentThread().getId();
        assertEquals(threadName, afterMethodExecution.getThreadName());
        assertEquals(threadId, afterMethodExecution.getThreadId());

        // since it uses fixed clock that returns same time, duration is 0
        assertEquals(Duration.ZERO, afterMethodExecution.getExecuteDuration());

        assertEquals(ProxyEventType.AFTER_METHOD, afterMethodExecution.getProxyEventType());

        assertSame(exception, afterMethodExecution.getThrown());
    }


    @Test
    void proceedExecutionWithNonPublisher() throws Throwable {

        // target method returns Batch (not Publisher)
        Method addMethod = ReflectionUtils.findMethod(Batch.class, "add", String.class);
        Batch target = mock(Batch.class);
        Object[] args = new Object[]{"QUERY"};
        LastExecutionAwareListener listener = new LastExecutionAwareListener();
        String connectionId = "conn-id";

        // produce single result in order to trigger StepVerifier#consumeNextWith.
        Batch mockBatch = mock(Batch.class);

        when(target.add("QUERY")).thenReturn(mockBatch);

        Object result = this.callbackSupport.proceedExecution(addMethod, target, args, listener, connectionId);

        // verify method on target is invoked
        verify(target).add("QUERY");

        assertSame(mockBatch, result);

        MethodExecutionInfo beforeMethodExecution = listener.getBeforeMethodExecutionInfo();
        MethodExecutionInfo afterMethodExecution = listener.getAfterMethodExecutionInfo();
        assertSame(beforeMethodExecution, afterMethodExecution);

        assertNull(listener.getBeforeQueryExecutionInfo());
        assertNull(listener.getAfterQueryExecutionInfo());

        assertEquals(target, afterMethodExecution.getTarget());
        assertEquals(mockBatch, afterMethodExecution.getResult());
        assertEquals(addMethod, afterMethodExecution.getMethod());
        assertEquals(args, afterMethodExecution.getMethodArgs());
        assertEquals(connectionId, afterMethodExecution.getConnectionId());

        String threadName = Thread.currentThread().getName();
        long threadId = Thread.currentThread().getId();
        assertEquals(threadName, afterMethodExecution.getThreadName());
        assertEquals(threadId, afterMethodExecution.getThreadId());

        // since it uses fixed clock that returns same time, duration is 0
        assertEquals(Duration.ZERO, afterMethodExecution.getExecuteDuration());

        assertEquals(ProxyEventType.AFTER_METHOD, afterMethodExecution.getProxyEventType());

        assertNull(afterMethodExecution.getThrown());
    }

    @Test
    void proceedExecutionWithNonPublisherThrowsException() throws Throwable {

        // target method returns Batch (not Publisher)
        Method addMethod = ReflectionUtils.findMethod(Batch.class, "add", String.class);
        Batch target = mock(Batch.class);
        Object[] args = new Object[]{"QUERY"};
        LastExecutionAwareListener listener = new LastExecutionAwareListener();
        String connectionId = "conn-id";

        // method invocation throws exception
        RuntimeException exception = new RuntimeException();
        when(target.add("QUERY")).thenThrow(exception);

        assertThrows(RuntimeException.class, () -> {
            this.callbackSupport.proceedExecution(addMethod, target, args, listener, connectionId);
        });

        verify(target).add("QUERY");


        MethodExecutionInfo beforeMethodExecution = listener.getBeforeMethodExecutionInfo();
        MethodExecutionInfo afterMethodExecution = listener.getAfterMethodExecutionInfo();
        assertSame(beforeMethodExecution, afterMethodExecution);

        assertNull(listener.getBeforeQueryExecutionInfo());
        assertNull(listener.getAfterQueryExecutionInfo());

        assertNull(afterMethodExecution.getResult());

        assertEquals(target, afterMethodExecution.getTarget());
        assertEquals(addMethod, afterMethodExecution.getMethod());
        assertEquals(args, afterMethodExecution.getMethodArgs());
        assertEquals(connectionId, afterMethodExecution.getConnectionId());

        String threadName = Thread.currentThread().getName();
        long threadId = Thread.currentThread().getId();
        assertEquals(threadName, afterMethodExecution.getThreadName());
        assertEquals(threadId, afterMethodExecution.getThreadId());

        // since it uses fixed clock that returns same time, duration is 0
        assertEquals(Duration.ZERO, afterMethodExecution.getExecuteDuration());

        assertEquals(ProxyEventType.AFTER_METHOD, afterMethodExecution.getProxyEventType());

        assertSame(exception, afterMethodExecution.getThrown());
    }

    @SuppressWarnings("unchecked")
    @Test
    void proceedExecutionWithConnectionFactoryCreateMethod() throws Throwable {

        // target method returns Producer
        Method createMethod = ReflectionUtils.findMethod(ConnectionFactory.class, "create");
        ConnectionFactory target = mock(ConnectionFactory.class);
        Object[] args = new Object[]{};
        LastExecutionAwareListener listener = new LastExecutionAwareListener();
        String connectionId = "conn-id";

        // produce single result in order to trigger StepVerifier#assertNext.
        Connection mockConnection = mock(Connection.class);
        Publisher<? extends Connection> publisher = Mono.just(mockConnection);
        doReturn(publisher).when(target).create();

        ConnectionIdManager connectionIdManager = mock(ConnectionIdManager.class);
        when(connectionIdManager.getId(mockConnection)).thenReturn(connectionId);
        when(this.proxyConfig.getConnectionIdManager()).thenReturn(connectionIdManager);


        Object result = this.callbackSupport.proceedExecution(createMethod, target, args, listener, null);

        // verify method on target is invoked
        verify(target).create();

        StepVerifier.create((Publisher<Tuple2<Connection, String>>) result)
                .expectSubscription()
                .assertNext(tuple2 -> {
                    // in middle of chain.
                    // at this point, beforeMethod has already called and special logic for
                    // "ConnectionFactory#create" has performed.
                    assertSame(mockConnection, tuple2.getT1());
                    assertEquals(connectionId, tuple2.getT2());

                    MethodExecutionInfo beforeMethod = listener.getBeforeMethodExecutionInfo();
                    assertNotNull(beforeMethod);
                    assertNull(listener.getAfterMethodExecutionInfo());

                    assertEquals(ProxyEventType.BEFORE_METHOD, beforeMethod.getProxyEventType());
                })
                .expectComplete()
                .verify();

        MethodExecutionInfo afterMethodExecution = listener.getAfterMethodExecutionInfo();
        assertEquals(connectionId, afterMethodExecution.getConnectionId());
        assertSame(target, afterMethodExecution.getTarget());
        assertSame(mockConnection, afterMethodExecution.getResult());

    }

}
