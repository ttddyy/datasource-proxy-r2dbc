package net.ttddyy.dsproxy.r2dbc;

import com.sun.jdi.ObjectReference;
import io.r2dbc.spi.Result;
import net.ttddyy.dsproxy.r2dbc.core.ExecutionInfo;
import net.ttddyy.dsproxy.r2dbc.core.ProxyDataSourceListener;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @author Tadaya Tsuyukubo
 */
public abstract class CallbackSupport {

    protected Clock clock = Clock.systemUTC();

    protected Object proceedExecution(Method method, Object target, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        }
    }

    protected Flux<? extends Result> interceptQueryExecution(Publisher<? extends Result> flux,
            ProxyDataSourceListener listener, ExecutionInfo executionInfo) {

        AtomicReference<Instant> startTimeHolder = new AtomicReference<>();

        // TODO: think about interceptor for doOnNext() -- each query execution
        // TODO: get thread id & name
        return Flux.empty()
                .ofType(Result.class)
                .doOnSubscribe(s -> {

                    Instant startTime = this.clock.instant();
                    startTimeHolder.set(startTime);

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

                    listener.afterQuery(executionInfo);
                });

    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }
}
