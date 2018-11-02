package net.ttddyy.dsproxy.r2dbc;

import io.r2dbc.spi.Result;
import net.ttddyy.dsproxy.r2dbc.core.ConnectionInfo;
import net.ttddyy.dsproxy.r2dbc.core.MethodExecutionInfo;
import net.ttddyy.dsproxy.r2dbc.core.ProxyEventType;
import net.ttddyy.dsproxy.r2dbc.core.ProxyExecutionListener;
import net.ttddyy.dsproxy.r2dbc.core.QueryExecutionInfo;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toSet;

/**
 * @author Tadaya Tsuyukubo
 */
public abstract class CallbackSupport {

    private static final Set<Method> PASS_THROUGH_METHODS;

    static {
        try {
            Method objectToStringMethod = Object.class.getMethod("toString");
            PASS_THROUGH_METHODS = Arrays.stream(Object.class.getMethods())
                    .filter(method -> !objectToStringMethod.equals(method))
                    .collect(toSet());

        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    protected Clock clock = Clock.systemUTC();

    protected ProxyConfig proxyConfig;


    public CallbackSupport(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    /**
     * Augment method invocation and call method listener.
     *
     * @param method         method to invoke on target
     * @param target         an object being invoked
     * @param args           arguments for the method
     * @param listener       listener that before/aftre method callbacks will be called
     * @param connectionInfo current connection information
     * @param onMap          a callback that will be chained on "map()" right after the result of the method invocation
     * @param onComplete     a callback that will be chained as the first doOnComplete on the result of the method invocation
     * @return
     * @throws Throwable
     */
    protected Object proceedExecution(Method method, Object target, Object[] args,
                                      ProxyExecutionListener listener, ConnectionInfo connectionInfo,
                                      BiFunction<Object, MethodExecutionInfo, Object> onMap,
                                      Consumer<MethodExecutionInfo> onComplete) throws Throwable {

        if (PASS_THROUGH_METHODS.contains(method)) {
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }

        // special handling for toString()
        if ("toString".equals(method.getName())) {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getSimpleName());   // ConnectionFactory, Connection, Batch, or Statement
            sb.append("-proxy [");
            sb.append(target.toString());
            sb.append("]");
            return sb.toString(); // differentiate toString message.
        }


        AtomicReference<Instant> startTimeHolder = new AtomicReference<>();

        MethodExecutionInfo executionInfo = new MethodExecutionInfo();
        executionInfo.setMethod(method);
        executionInfo.setMethodArgs(args);
        executionInfo.setTarget(target);
        executionInfo.setConnectionInfo(connectionInfo);

        Class<?> returnType = method.getReturnType();

        if (Publisher.class.isAssignableFrom(returnType)) {

            Publisher<?> result;
            try {
                result = (Publisher<?>) method.invoke(target, args);
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }

            return Flux.empty()
                    .doOnSubscribe(s -> {

                        Instant startTime = this.clock.instant();
                        startTimeHolder.set(startTime);

                        String threadName = Thread.currentThread().getName();
                        long threadId = Thread.currentThread().getId();
                        executionInfo.setThreadName(threadName);
                        executionInfo.setThreadId(threadId);

                        executionInfo.setProxyEventType(ProxyEventType.BEFORE_METHOD);

                        listener.onMethodExecution(executionInfo);
                    })
                    .concatWith(result)
                    .map(resultObj -> {

                        // set produced object as result
                        executionInfo.setResult(resultObj);

                        // apply a function to flux-chain right after the original publisher operations
                        if (onMap != null) {
                            return onMap.apply(resultObj, executionInfo);
                        }
                        return resultObj;
                    })
                    .doOnComplete(() -> {
                        // apply a consumer to flux-chain right after the original publisher operations
                        // this is the first chained doOnComplete on the result publisher
                        if (onComplete != null) {
                            onComplete.accept(executionInfo);
                        }
                    })
                    .doOnError(throwable -> {
                        executionInfo.setThrown(throwable);
                    })
                    .doFinally(signalType -> {

                        Instant startTime = startTimeHolder.get();
                        Instant currentTime = this.clock.instant();

                        Duration executionDuration = Duration.between(startTime, currentTime);
                        executionInfo.setExecuteDuration(executionDuration);

                        String threadName = Thread.currentThread().getName();
                        long threadId = Thread.currentThread().getId();
                        executionInfo.setThreadName(threadName);
                        executionInfo.setThreadId(threadId);

                        executionInfo.setProxyEventType(ProxyEventType.AFTER_METHOD);

                        listener.onMethodExecution(executionInfo);
                    });


        } else {
            // for method that generates non-publisher, execution happens when it is invoked.

            String threadName = Thread.currentThread().getName();
            long threadId = Thread.currentThread().getId();
            executionInfo.setThreadName(threadName);
            executionInfo.setThreadId(threadId);

            // invoke before method
            executionInfo.setProxyEventType(ProxyEventType.BEFORE_METHOD);
            listener.onMethodExecution(executionInfo);

            Instant startTime = this.clock.instant();

            Object result = null;
            Throwable thrown = null;
            try {
                result = method.invoke(target, args);
            } catch (InvocationTargetException ex) {
                thrown = ex.getTargetException();
                throw thrown;
            } finally {
                executionInfo.setResult(result);
                executionInfo.setThrown(thrown);

                Instant currentTime = this.clock.instant();
                Duration executionDuration = Duration.between(startTime, currentTime);
                executionInfo.setExecuteDuration(executionDuration);

                executionInfo.setProxyEventType(ProxyEventType.AFTER_METHOD);
                listener.onMethodExecution(executionInfo);
            }
            return result;

        }

    }

    /**
     * Augment query execution result to hook up listener lifecycle.
     */
    protected Flux<? extends Result> interceptQueryExecution(Publisher<? extends Result> flux,
                                                             ProxyExecutionListener listener, QueryExecutionInfo executionInfo) {

        AtomicReference<Instant> startTimeHolder = new AtomicReference<>();

        AtomicInteger resultCount = new AtomicInteger(0);

        return Flux.empty()
                .ofType(Result.class)
                .doOnSubscribe(s -> {

                    Instant startTime = this.clock.instant();
                    startTimeHolder.set(startTime);

                    String threadName = Thread.currentThread().getName();
                    long threadId = Thread.currentThread().getId();
                    executionInfo.setThreadName(threadName);
                    executionInfo.setThreadId(threadId);

                    executionInfo.setCurrentResult(null);

                    executionInfo.setProxyEventType(ProxyEventType.BEFORE_QUERY);

                    listener.onQueryExecution(executionInfo);
                })
                .concatWith(flux)
                .doOnNext(result -> {
                    // on each query result

                    Instant startTime = startTimeHolder.get();
                    Instant currentTime = this.clock.instant();

                    Duration executionDuration = Duration.between(startTime, currentTime);
                    executionInfo.setExecuteDuration(executionDuration);

                    String threadName = Thread.currentThread().getName();
                    long threadId = Thread.currentThread().getId();
                    executionInfo.setThreadName(threadName);
                    executionInfo.setThreadId(threadId);

                    executionInfo.setProxyEventType(ProxyEventType.EACH_QUERY_RESULT);

                    executionInfo.setCurrentResult(result);

                    int count = resultCount.incrementAndGet();
                    executionInfo.setCurrentResultCount(count);

                    listener.eachQueryResult(executionInfo);

                })
                .doOnComplete(() -> {
                    executionInfo.setSuccess(true);
                })
                .doOnError(throwable -> {
                    executionInfo.setThrowable(throwable);
                    executionInfo.setSuccess(false);
                })
                .doFinally(signalType -> {

                    Instant startTime = startTimeHolder.get();
                    Instant currentTime = this.clock.instant();

                    Duration executionDuration = Duration.between(startTime, currentTime);
                    executionInfo.setExecuteDuration(executionDuration);

                    String threadName = Thread.currentThread().getName();
                    long threadId = Thread.currentThread().getId();
                    executionInfo.setThreadName(threadName);
                    executionInfo.setThreadId(threadId);

                    executionInfo.setCurrentResult(null);

                    executionInfo.setProxyEventType(ProxyEventType.AFTER_QUERY);

                    listener.onQueryExecution(executionInfo);
                });

    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }
}
