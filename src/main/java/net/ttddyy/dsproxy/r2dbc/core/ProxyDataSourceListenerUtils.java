package net.ttddyy.dsproxy.r2dbc.core;

import io.r2dbc.spi.Result;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

/**
 *
 * @author Tadaya Tsuyukubo
 */
public class ProxyDataSourceListenerUtils {

    // TODO: rename
    public static Flux<? extends Result> interceptQueryExecution(Publisher<? extends Result> flux,
            ProxyDataSourceListener listener, ExecutionInfo executionInfo) {

        // TODO: get thread id & name
        return Flux.empty()
                .ofType(Result.class)
                .doOnSubscribe(s -> {
                    System.out.println("AAA BEFORE Execution");
                    listener.beforeQuery(executionInfo);
                })
                .concatWith(flux)
                .doOnNext(result -> {
                    executionInfo.setResult(result);
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
