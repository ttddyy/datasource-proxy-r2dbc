package net.ttddyy.dsproxy.r2dbc.proxy;

import io.r2dbc.spi.Result;
import net.ttddyy.dsproxy.r2dbc.core.CompositeProxyExecutionListener;
import net.ttddyy.dsproxy.r2dbc.core.ProxyEventType;
import net.ttddyy.dsproxy.r2dbc.core.QueryExecutionInfo;
import net.ttddyy.dsproxy.r2dbc.support.LastExecutionAwareListener;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.lang.reflect.Method;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Tadaya Tsuyukubo
 */
public class ReactiveResultCallbackTest {

    private static Method MAP_METHOD = ReflectionUtils.findMethod(Result.class, "map", BiFunction.class);
    private static Method GET_TARGET_METHOD = ReflectionUtils.findMethod(ProxyObject.class, "getTarget");

    @Test
    void map() throws Throwable {
        LastExecutionAwareListener listener = new LastExecutionAwareListener();
        CompositeProxyExecutionListener compositeListener = new CompositeProxyExecutionListener(listener);

        QueryExecutionInfo queryExecutionInfo = new QueryExecutionInfo();
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.addListener(compositeListener);

        Publisher<Object> source = Flux.just("foo", "bar", "baz");
        Result mockResult = mock(Result.class);
        when(mockResult.map(any())).thenReturn(source);

        ReactiveResultCallback callback = new ReactiveResultCallback(mockResult, queryExecutionInfo, proxyConfig);

        // since "mockResult.map()" is mocked, args can be anything as long as num of args matches to signature.
        Object[] args = new Object[]{null};
        Object result = callback.invoke(null, MAP_METHOD, args);

        assertThat(result)
                .isInstanceOf(Publisher.class)
                .isNotSameAs(source);

        long threadId = Thread.currentThread().getId();
        String threadName = Thread.currentThread().getName();

        StepVerifier.create((Publisher<?>) result)
                .expectSubscription()
                .assertNext(obj -> {  // first
                    assertEquals("foo", obj);
                    assertSame(queryExecutionInfo, listener.getEachQueryResultExecutionInfo());

                    // verify EACH_QUERY_RESULT
                    assertEquals(ProxyEventType.EACH_QUERY_RESULT, queryExecutionInfo.getProxyEventType());
                    assertEquals(1, queryExecutionInfo.getCurrentResultCount());
                    assertEquals("foo", queryExecutionInfo.getCurrentMappedResult());
                    assertEquals(threadId, queryExecutionInfo.getThreadId());
                    assertEquals(threadName, queryExecutionInfo.getThreadName());
                    assertNull(queryExecutionInfo.getThrowable());
                })
                .assertNext(obj -> {  // second
                    assertEquals("bar", obj);
                    assertSame(queryExecutionInfo, listener.getEachQueryResultExecutionInfo());

                    // verify EACH_QUERY_RESULT
                    assertEquals(ProxyEventType.EACH_QUERY_RESULT, queryExecutionInfo.getProxyEventType());
                    assertEquals(2, queryExecutionInfo.getCurrentResultCount());
                    assertEquals("bar", queryExecutionInfo.getCurrentMappedResult());
                    assertEquals(threadId, queryExecutionInfo.getThreadId());
                    assertEquals(threadName, queryExecutionInfo.getThreadName());
                    assertNull(queryExecutionInfo.getThrowable());
                })
                .assertNext(obj -> {  // third
                    assertEquals("baz", obj);
                    assertSame(queryExecutionInfo, listener.getEachQueryResultExecutionInfo());

                    // verify EACH_QUERY_RESULT
                    assertEquals(ProxyEventType.EACH_QUERY_RESULT, queryExecutionInfo.getProxyEventType());
                    assertEquals(3, queryExecutionInfo.getCurrentResultCount());
                    assertEquals("baz", queryExecutionInfo.getCurrentMappedResult());
                    assertEquals(threadId, queryExecutionInfo.getThreadId());
                    assertEquals(threadName, queryExecutionInfo.getThreadName());
                    assertNull(queryExecutionInfo.getThrowable());
                })
                .verifyComplete();

    }

    @Test
    void mapWithPublisherException() throws Throwable {
        LastExecutionAwareListener listener = new LastExecutionAwareListener();
        CompositeProxyExecutionListener compositeListener = new CompositeProxyExecutionListener(listener);

        QueryExecutionInfo queryExecutionInfo = new QueryExecutionInfo();
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.addListener(compositeListener);


        // return a publisher that throws exception at execution
        Exception exception = new RuntimeException("map exception");
        TestPublisher<Object> publisher = TestPublisher.create().error(exception);

        Result mockResult = mock(Result.class);
        when(mockResult.map(any())).thenReturn(publisher);

        ReactiveResultCallback callback = new ReactiveResultCallback(mockResult, queryExecutionInfo, proxyConfig);

        // since "mockResult.map()" is mocked, args can be anything as long as num of args matches to signature.
        Object[] args = new Object[]{null};
        Object result = callback.invoke(null, MAP_METHOD, args);

        assertThat(result).isInstanceOf(Publisher.class);
        assertNotSame(publisher, result);

        long threadId = Thread.currentThread().getId();
        String threadName = Thread.currentThread().getName();

        StepVerifier.create((Publisher<?>) result)
                .expectSubscription()
                .consumeErrorWith(thrown -> {
                    assertSame(exception, thrown);
                })
                .verify();

        assertSame(queryExecutionInfo, listener.getEachQueryResultExecutionInfo());

        // verify EACH_QUERY_RESULT
        assertEquals(ProxyEventType.EACH_QUERY_RESULT, queryExecutionInfo.getProxyEventType());
        assertEquals(1, queryExecutionInfo.getCurrentResultCount());
        assertNull(queryExecutionInfo.getCurrentMappedResult());
        assertEquals(threadId, queryExecutionInfo.getThreadId());
        assertEquals(threadName, queryExecutionInfo.getThreadName());
    }

    @Test
    void mapWithEmptyPublisher() throws Throwable {
        LastExecutionAwareListener listener = new LastExecutionAwareListener();
        CompositeProxyExecutionListener compositeListener = new CompositeProxyExecutionListener(listener);

        QueryExecutionInfo queryExecutionInfo = new QueryExecutionInfo();
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.addListener(compositeListener);

        // return empty publisher
        Publisher<Object> publisher = Flux.empty();
        Result mockResult = mock(Result.class);
        when(mockResult.map(any())).thenReturn(publisher);

        ReactiveResultCallback callback = new ReactiveResultCallback(mockResult, queryExecutionInfo, proxyConfig);

        // since "mockResult.map()" is mocked, args can be anything as long as num of args matches to signature.
        Object[] args = new Object[]{null};
        Object result = callback.invoke(null, MAP_METHOD, args);

        assertThat(result)
                .isInstanceOf(Publisher.class)
                .isNotSameAs(publisher);

        StepVerifier.create((Publisher<?>) result)
                .expectSubscription()
                .verifyComplete();

        assertNull(listener.getEachQueryResultExecutionInfo(),
                "EachQueryResult callback should not be called");
    }

    @Test
    void mapWithResultThatErrorsAtExecutionTime() throws Throwable {

        // call to the "map()" method returns a publisher that fails(errors) at execution time

        LastExecutionAwareListener listener = new LastExecutionAwareListener();
        CompositeProxyExecutionListener compositeListener = new CompositeProxyExecutionListener(listener);

        QueryExecutionInfo queryExecutionInfo = new QueryExecutionInfo();
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.addListener(compositeListener);


        RuntimeException exception = new RuntimeException("failure");

        // publisher that fails at execution
        Publisher<Object> source = Flux.just("foo", "bar", "baz")
                .map(str -> {
                    throw exception;
                });

        Result mockResult = mock(Result.class);
        when(mockResult.map(any())).thenReturn(source);

        ReactiveResultCallback callback = new ReactiveResultCallback(mockResult, queryExecutionInfo, proxyConfig);

        // since "mockResult.map()" is mocked, args can be anything as long as num of args matches to signature.
        Object[] args = new Object[]{null};
        Object result = callback.invoke(null, MAP_METHOD, args);

        assertThat(result)
                .isInstanceOf(Publisher.class)
                .isNotSameAs(source);

        Flux<String> resultConsumer = Flux.from((Publisher<String>) result);

        long threadId = Thread.currentThread().getId();
        String threadName = Thread.currentThread().getName();

        StepVerifier.create(resultConsumer)
                .expectSubscription()
                .consumeErrorWith(thrown -> {
                    assertSame(exception, thrown);
                })
                .verify();

        // verify callback
        assertSame(queryExecutionInfo, listener.getEachQueryResultExecutionInfo(),
                "listener should be called even consuming throws exception");
        assertEquals(ProxyEventType.EACH_QUERY_RESULT, queryExecutionInfo.getProxyEventType());
        assertEquals(1, queryExecutionInfo.getCurrentResultCount());
        assertNull(queryExecutionInfo.getCurrentMappedResult());
        assertSame(exception, queryExecutionInfo.getThrowable());
        assertEquals(threadId, queryExecutionInfo.getThreadId());
        assertEquals(threadName, queryExecutionInfo.getThreadName());

    }

    @Test
    void getTarget() throws Throwable {
        Result mockResult = mock(Result.class);
        QueryExecutionInfo queryExecutionInfo = new QueryExecutionInfo();
        ProxyConfig proxyConfig = new ProxyConfig();

        ReactiveResultCallback callback = new ReactiveResultCallback(mockResult, queryExecutionInfo, proxyConfig);

        Object result = callback.invoke(null, GET_TARGET_METHOD, null);
        assertSame(mockResult, result);
    }

}
