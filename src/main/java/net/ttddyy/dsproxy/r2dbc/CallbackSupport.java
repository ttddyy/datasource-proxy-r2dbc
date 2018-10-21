package net.ttddyy.dsproxy.r2dbc;

import io.r2dbc.spi.Result;
import net.ttddyy.dsproxy.r2dbc.core.ExecutionInfo;
import net.ttddyy.dsproxy.r2dbc.core.ProxyDataSourceListener;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *
 * @author Tadaya Tsuyukubo
 */
public abstract class CallbackSupport {

    protected static Object proceedExecution(Method method, Object target, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        }
    }

    protected static Flux<? extends Result> interceptQueryExecution(Publisher<? extends Result> flux,
            ProxyDataSourceListener listener, ExecutionInfo executionInfo) {

        // TODO: think about interceptor for doOnNext() -- each query execution
        // TODO: get thread id & name
        return Flux.empty()
                .ofType(Result.class)
                .doOnSubscribe(s -> {
                    System.out.println("AAA BEFORE Execution");
                    listener.beforeQuery(executionInfo);
                })
                .concatWith(flux)
                .doOnComplete(() -> {
                    executionInfo.setSuccess(true);
                })
                .doOnError(throwable -> {
                    executionInfo.setThrowable(throwable);
                    executionInfo.setSuccess(false);
                })
                .doFinally(signalType -> {
                    // TODO: populate execution time
                    System.out.println("AAA AFTER Execution");
                    listener.afterQuery(executionInfo);
                });

    }

}
