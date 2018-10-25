package net.ttddyy.dsproxy.r2dbc;

import io.r2dbc.spi.Result;
import net.ttddyy.dsproxy.r2dbc.core.MethodExecutionInfo;
import net.ttddyy.dsproxy.r2dbc.core.ProxyDataSourceListener;
import net.ttddyy.dsproxy.r2dbc.core.QueryExecutionInfo;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Tadaya Tsuyukubo
 */
public abstract class CallbackSupport {

    protected Clock clock = Clock.systemUTC();

    /**
     * Augment method invocation and call method listener.
     */
    protected Object proceedExecution(Method method, Object target, Object[] args, ProxyDataSourceListener listener, String connectionId) throws Throwable {
        AtomicReference<Instant> startTimeHolder = new AtomicReference<>();

        MethodExecutionInfo executionInfo = new MethodExecutionInfo();
        executionInfo.setMethod(method);
        executionInfo.setMethodArgs(args);
        executionInfo.setTarget(target);
        executionInfo.setConnectionId(connectionId);

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

                        listener.beforeMethod(executionInfo);
                    })
                    .concatWith(result)
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

                        listener.afterMethod(executionInfo);
                    });


        } else {

            listener.beforeMethod(executionInfo);

            Instant startTime = this.clock.instant();

            Object result = null;
            Throwable thrown = null;
            try {
                result = method.invoke(target, args);
            } catch (InvocationTargetException ex) {
                thrown = ex;
                throw ex.getTargetException();
            } catch (Throwable throwable) {
                thrown = throwable;
                throw throwable;
            } finally {
                executionInfo.setResult(result);
                executionInfo.setThrown(thrown);

                Instant currentTime = this.clock.instant();
                Duration executionDuration = Duration.between(startTime, currentTime);
                executionInfo.setExecuteDuration(executionDuration);

                listener.afterMethod(executionInfo);
            }
            return result;

        }

    }

    /**
     * Augment query execution result to hook up listener lifecycle.
     */
    protected Flux<? extends Result> interceptQueryExecution(Publisher<? extends Result> flux,
                                                             ProxyDataSourceListener listener, QueryExecutionInfo executionInfo) {

        AtomicReference<Instant> startTimeHolder = new AtomicReference<>();

        // TODO: think about interceptor for doOnNext() -- each query execution
        return Flux.empty()
                .ofType(Result.class)
                .doOnSubscribe(s -> {

                    Instant startTime = this.clock.instant();
                    startTimeHolder.set(startTime);

                    String threadName = Thread.currentThread().getName();
                    long threadId = Thread.currentThread().getId();
                    executionInfo.setThreadName(threadName);
                    executionInfo.setThreadId(threadId);

                    listener.beforeQuery(executionInfo);
                })
                .concatWith(flux)
                .doOnNext(result -> {
                    // TODO: add listener callback for each query result
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

                    listener.afterQuery(executionInfo);
                });

    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }
}
