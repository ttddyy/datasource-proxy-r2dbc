package net.ttddyy.dsproxy.r2dbc.proxy;

import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Result;
import net.ttddyy.dsproxy.r2dbc.core.CompositeProxyExecutionListener;
import net.ttddyy.dsproxy.r2dbc.core.ConnectionInfo;
import net.ttddyy.dsproxy.r2dbc.core.MethodExecutionInfo;
import net.ttddyy.dsproxy.r2dbc.core.ProxyEventType;
import net.ttddyy.dsproxy.r2dbc.core.QueryExecutionInfo;
import net.ttddyy.dsproxy.r2dbc.support.LastExecutionAwareListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Publisher;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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


    @Test
    void interceptQueryExecution() {

        LastExecutionAwareListener listener = new LastExecutionAwareListener();
        QueryExecutionInfo executionInfo = new QueryExecutionInfo();

        ProxyFactory proxyFactory = mock(ProxyFactory.class);

        CompositeProxyExecutionListener compositeListener = new CompositeProxyExecutionListener(listener);
        when(this.proxyConfig.getListeners()).thenReturn(compositeListener);
        when(this.proxyConfig.getProxyFactory()).thenReturn(proxyFactory);

        // when it creates a proxy for Result
        Result mockResultProxy = mock(Result.class);
        when(proxyFactory.createResult(any(), any())).thenReturn(mockResultProxy);

        // produce single result
        Result mockResult = mock(Result.class);
        Mono<Result> resultPublisher = Mono.just(mockResult);

        Flux<? extends Result> result = this.callbackSupport.interceptQueryExecution(resultPublisher, executionInfo);

        // verifies result flux
        StepVerifier.create(result)
                .expectSubscription()
                .consumeNextWith(c -> {
                    // verify produced result is the proxy result
                    assertSame(mockResultProxy, c);
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

        assertEquals(0, executionInfo.getCurrentResultCount());
        assertNull(executionInfo.getCurrentMappedResult());


        // verify the call to create a proxy result
        ArgumentCaptor<Result> resultCaptor = ArgumentCaptor.forClass(Result.class);
        verify(proxyFactory).createResult(resultCaptor.capture(), eq(executionInfo));

        Result captureResult = resultCaptor.getValue();
        assertSame(mockResult, captureResult);

    }

    @Test
    void interceptQueryExecutionWithFailure() {

        LastExecutionAwareListener listener = new LastExecutionAwareListener();
        QueryExecutionInfo executionInfo = new QueryExecutionInfo();

        CompositeProxyExecutionListener compositeListener = new CompositeProxyExecutionListener(listener);
        when(this.proxyConfig.getListeners()).thenReturn(compositeListener);

        // publisher that throws exception
        RuntimeException exception = new RuntimeException();
        Publisher<Result> publisher = TestPublisher.<Result>create().error(exception);

        Flux<? extends Result> result = this.callbackSupport.interceptQueryExecution(publisher, executionInfo);

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

        assertEquals(0, executionInfo.getCurrentResultCount());
    }

    @Test
    void interceptQueryExecutionWithMultipleResult() {

        LastExecutionAwareListener listener = new LastExecutionAwareListener();
        QueryExecutionInfo executionInfo = new QueryExecutionInfo();

        ProxyFactory proxyFactory = mock(ProxyFactory.class);

        CompositeProxyExecutionListener compositeListener = new CompositeProxyExecutionListener(listener);
        when(this.proxyConfig.getListeners()).thenReturn(compositeListener);
        when(this.proxyConfig.getProxyFactory()).thenReturn(proxyFactory);

        // when it creates a proxy for Result
        Result mockResultProxy = mock(Result.class);
        when(proxyFactory.createResult(any(), any())).thenReturn(mockResultProxy);

        // produce multiple results
        Result mockResult1 = mock(Result.class);
        Result mockResult2 = mock(Result.class);
        Result mockResult3 = mock(Result.class);
        Flux<Result> publisher = Flux.just(mockResult1, mockResult2, mockResult3)
                .doOnSubscribe(subscription -> {
                    // this will be called AFTER listener.beforeQuery() but BEFORE emitting query result from this publisher.
                    // verify BEFORE_QUERY
                    assertEquals(ProxyEventType.BEFORE_QUERY, executionInfo.getProxyEventType());
                    assertSame(executionInfo, listener.getBeforeQueryExecutionInfo());

                    assertEquals(0, executionInfo.getCurrentResultCount());
                    assertNull(executionInfo.getCurrentMappedResult());
                });

        Flux<? extends Result> result = this.callbackSupport.interceptQueryExecution(publisher, executionInfo);

        // result should return a publisher that emits 3 proxy results
        // verifies result flux
        StepVerifier.create(result)
                .expectSubscription()
                .assertNext(c -> {
                    assertSame(mockResultProxy, c, "first result");
                })
                .assertNext(c -> {
                    assertSame(mockResultProxy, c, "second result");
                })
                .assertNext(c -> {
                    assertSame(mockResultProxy, c, "third result");

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

        assertEquals(0, executionInfo.getCurrentResultCount());
        assertNull(executionInfo.getCurrentMappedResult());


        // verify the call to create proxy result
        ArgumentCaptor<Result> resultCaptor = ArgumentCaptor.forClass(Result.class);
        verify(proxyFactory, times(3)).createResult(resultCaptor.capture(), eq(executionInfo));

        List<Result> captured = resultCaptor.getAllValues();
        assertThat(captured).hasSize(3).containsExactly(mockResult1, mockResult2, mockResult3);
    }

    @Test
    void interceptQueryExecutionWithEmptyResult() {

        LastExecutionAwareListener listener = new LastExecutionAwareListener();
        QueryExecutionInfo executionInfo = new QueryExecutionInfo();

        CompositeProxyExecutionListener compositeListener = new CompositeProxyExecutionListener(listener);
        when(this.proxyConfig.getListeners()).thenReturn(compositeListener);

        // produce multiple results
        Flux<Result> publisher = Flux.<Result>empty()
                .doOnSubscribe(subscription -> {
                    // this will be called AFTER listener.beforeQuery() but BEFORE emitting query result from this publisher.
                    // verify BEFORE_QUERY
                    assertEquals(ProxyEventType.BEFORE_QUERY, executionInfo.getProxyEventType());
                    assertSame(executionInfo, listener.getBeforeQueryExecutionInfo());

                    assertEquals(0, executionInfo.getCurrentResultCount());
                    assertNull(executionInfo.getCurrentMappedResult());
                });
        ;

        Flux<? extends Result> result = this.callbackSupport.interceptQueryExecution(publisher, executionInfo);

        // verifies result flux
        StepVerifier.create(result)
                .expectSubscription()
                .expectNextCount(0)
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

        assertEquals(0, executionInfo.getCurrentResultCount());
        assertNull(executionInfo.getCurrentMappedResult());

    }


    @SuppressWarnings("unchecked")
    @Test
    void proceedExecutionWithPublisher() throws Throwable {

        // target method returns Producer
        Method executeMethod = ReflectionUtils.findMethod(Batch.class, "execute");
        Batch target = mock(Batch.class);
        Object[] args = new Object[]{};
        LastExecutionAwareListener listener = new LastExecutionAwareListener();
        ConnectionInfo connectionInfo = new ConnectionInfo();

        // produce single result in order to trigger StepVerifier#consumeNextWith.
        Result mockResult = mock(Result.class);
        Mono<Result> publisher = Mono.just(mockResult);

        when(target.execute()).thenReturn(publisher);

        Object result = this.callbackSupport.proceedExecution(executeMethod, target, args, listener, connectionInfo, null, null);

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
        assertSame(connectionInfo, afterMethodExecution.getConnectionInfo());

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
        ConnectionInfo connectionInfo = new ConnectionInfo();

        // publisher that throws exception
        RuntimeException exception = new RuntimeException();
        Publisher<Result> publisher = TestPublisher.<Result>create().error(exception);

        when(target.execute()).thenReturn(publisher);

        Object result = this.callbackSupport.proceedExecution(executeMethod, target, args, listener, connectionInfo, null, null);

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
        assertEquals(connectionInfo, afterMethodExecution.getConnectionInfo());

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
        ConnectionInfo connectionInfo = new ConnectionInfo();

        // produce single result in order to trigger StepVerifier#consumeNextWith.
        Batch mockBatch = mock(Batch.class);

        when(target.add("QUERY")).thenReturn(mockBatch);

        Object result = this.callbackSupport.proceedExecution(addMethod, target, args, listener, connectionInfo, null, null);

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
        assertSame(connectionInfo, afterMethodExecution.getConnectionInfo());

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
        ConnectionInfo connectionInfo = new ConnectionInfo();

        // method invocation throws exception
        RuntimeException exception = new RuntimeException();
        when(target.add("QUERY")).thenThrow(exception);

        assertThrows(RuntimeException.class, () -> {
            this.callbackSupport.proceedExecution(addMethod, target, args, listener, connectionInfo, null, null);
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
        assertEquals(connectionInfo, afterMethodExecution.getConnectionInfo());

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
    void proceedExecutionWithToString_HashCode_Equals_Methods() throws Throwable {

        class MyStub {
            @Override
            public String toString() {
                return "FOO";
            }
        }

        // target method returns Producer
        Method toStringMethod = ReflectionUtils.findMethod(Object.class, "toString");
        Method hashCodeMethod = ReflectionUtils.findMethod(Object.class, "hashCode");
        Method equalsMethod = ReflectionUtils.findMethod(Object.class, "equals", Object.class);
        MyStub target = new MyStub();
        LastExecutionAwareListener listener = new LastExecutionAwareListener();

        Object result;

        // verify toString()
        result = this.callbackSupport.proceedExecution(toStringMethod, target, null, listener, null, null, null);
        assertEquals("MyStub-proxy [FOO]", result);

        // verify hashCode()
        result = this.callbackSupport.proceedExecution(hashCodeMethod, target, null, listener, null, null, null);
        assertEquals(target.hashCode(), result);

        // verify equals() with null
        result = this.callbackSupport.proceedExecution(equalsMethod, target, new Object[]{null}, listener, null, null, null);
        assertThat(result).isEqualTo(false);

        // verify equals() with target
        result = this.callbackSupport.proceedExecution(equalsMethod, target, new Object[]{target}, listener, null, null, null);
        assertThat(result).isEqualTo(true);
    }

}
