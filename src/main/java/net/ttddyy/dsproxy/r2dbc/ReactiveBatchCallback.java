package net.ttddyy.dsproxy.r2dbc;

import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Result;
import net.ttddyy.dsproxy.r2dbc.core.ConnectionInfo;
import net.ttddyy.dsproxy.r2dbc.core.ExecutionType;
import net.ttddyy.dsproxy.r2dbc.core.QueryExecutionInfo;
import net.ttddyy.dsproxy.r2dbc.core.QueryInfo;
import org.reactivestreams.Publisher;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Proxy callback for {@link Batch}.
 *
 * @author Tadaya Tsuyukubo
 */
public class ReactiveBatchCallback extends CallbackSupport {

    private Batch<?> batch;

    private ConnectionInfo connectionInfo;
    private List<String> queries = new ArrayList<>();

    public ReactiveBatchCallback(Batch<?> batch, ConnectionInfo connectionInfo, ProxyConfig proxyConfig) {
        super(proxyConfig);
        this.batch = batch;
        this.connectionInfo = connectionInfo;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        String methodName = method.getName();

        if ("getTarget".equals(methodName)) {
            return this.batch;
        } else if ("getOriginalConnection".equals(methodName)) {
            return this.connectionInfo.getOriginalConnection();
        }

        Object result = proceedExecution(method, this.batch, args, this.proxyConfig.getListeners(), this.connectionInfo, null, null);

        if ("add".equals(methodName)) {
            this.queries.add((String) args[0]);
        } else if ("execute".equals(methodName)) {

            List<QueryInfo> queryInfoList = this.queries.stream()
                    .map(QueryInfo::new)
                    .collect(toList());

            QueryExecutionInfo execInfo = new QueryExecutionInfo();
            execInfo.setType(ExecutionType.BATCH);
            execInfo.setQueries(queryInfoList);
            execInfo.setBatchSize(this.queries.size());
            execInfo.setMethod(method);
            execInfo.setMethodArgs(args);
            execInfo.setConnectionInfo(this.connectionInfo);

            // API defines "execute()" returns a publisher
            Publisher<? extends Result> publisher = (Publisher<? extends Result>) result;

            return interceptQueryExecution(publisher, proxyConfig.getListeners(), execInfo);
        }

        return result;
    }
}
