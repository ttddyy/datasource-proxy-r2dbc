package net.ttddyy.dsproxy.r2dbc.proxy;

import io.r2dbc.spi.Result;
import net.ttddyy.dsproxy.r2dbc.core.ConnectionInfo;
import net.ttddyy.dsproxy.r2dbc.core.ProxyEventType;
import net.ttddyy.dsproxy.r2dbc.core.QueryExecutionInfo;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Proxy callback for {@link Result}.
 *
 * @author Tadaya Tsuyukubo
 */
public class ReactiveResultCallback extends CallbackSupport {

    private Result result;
    private QueryExecutionInfo queryExecutionInfo;

    public ReactiveResultCallback(Result result, QueryExecutionInfo queryExecutionInfo, ProxyConfig proxyConfig) {
        super(proxyConfig);
        this.result = result;
        this.queryExecutionInfo = queryExecutionInfo;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        String methodName = method.getName();
        ConnectionInfo connectionInfo = this.queryExecutionInfo.getConnectionInfo();

        if ("getTarget".equals(methodName)) {  // for ProxyObject
            return this.result;
        } else if ("getOriginalConnection".equals(methodName)) {  // for ConnectionHolder
            return connectionInfo.getOriginalConnection();
        }


        Object invocationResult = proceedExecution(method, this.result, args, this.proxyConfig.getListeners(), connectionInfo, null, null);

        if ("map".equals(methodName)) {

            AtomicInteger resultCount = new AtomicInteger(0);

            // add logic to call "listener#eachQueryResult()"
            return Flux.from((Publisher<?>) invocationResult)
                    .doOnEach(signal -> {

                        boolean proceed = signal.isOnNext() || signal.isOnError();
                        if (!proceed) {
                            return;
                        }

                        int count = resultCount.incrementAndGet();

                        if (signal.isOnNext()) {
                            Object mappedResult = signal.get();

                            this.queryExecutionInfo.setCurrentResultCount(count);
                            this.queryExecutionInfo.setCurrentMappedResult(mappedResult);
                            this.queryExecutionInfo.setThrowable(null);
                        } else {
                            // onError
                            Throwable thrown = signal.getThrowable();
                            this.queryExecutionInfo.setCurrentResultCount(count);
                            this.queryExecutionInfo.setCurrentMappedResult(null);
                            this.queryExecutionInfo.setThrowable(thrown);

                        }

                        this.queryExecutionInfo.setProxyEventType(ProxyEventType.EACH_QUERY_RESULT);

                        String threadName = Thread.currentThread().getName();
                        long threadId = Thread.currentThread().getId();
                        this.queryExecutionInfo.setThreadName(threadName);
                        this.queryExecutionInfo.setThreadId(threadId);

                        // callback
                        this.proxyConfig.getListeners().eachQueryResult(this.queryExecutionInfo);

                    });

        }


        return invocationResult;

    }

}
