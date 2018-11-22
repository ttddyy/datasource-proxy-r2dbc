package net.ttddyy.dsproxy.r2dbc.proxy;

import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Statement;
import net.ttddyy.dsproxy.r2dbc.core.ConnectionInfo;
import net.ttddyy.dsproxy.r2dbc.core.MethodExecutionInfo;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Proxy callback for {@link Connection}.
 *
 * @author Tadaya Tsuyukubo
 */
public class ReactiveConnectionCallback extends CallbackSupport {

    private Connection connection;
    private ConnectionInfo connectionInfo;

    public ReactiveConnectionCallback(Connection connection, ConnectionInfo connectionInfo, ProxyConfig proxyConfig) {
        super(proxyConfig);
        this.connection = connection;
        this.connectionInfo = connectionInfo;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        String methodName = method.getName();

        if ("getTarget".equals(methodName)) {
            return this.connection;
        } else if ("getOriginalConnection".equals(methodName)) {
            return this.connection;
        }

        Consumer<MethodExecutionInfo> onComplete = null;

        // since these methods return Publisher<Void> pass the callback for doOnComplete().
        if ("beginTransaction".equals(methodName)) {
            onComplete = executionInfo -> {
                executionInfo.getConnectionInfo().incrementTransactionCount();
            };
        } else if ("commitTransaction".equals(methodName)) {
            onComplete = executionInfo -> {
                executionInfo.getConnectionInfo().incrementCommitCount();
            };
        } else if ("rollbackTransaction".equals(methodName)) {
            onComplete = executionInfo -> {
                executionInfo.getConnectionInfo().incrementRollbackCount();
            };
        } else if ("close".equals(methodName)) {
            onComplete = executionInfo -> {
                executionInfo.getConnectionInfo().setClosed(true);
            };
        }
        // TODO: createSavepoint, releaseSavepoint, rollbackTransactionToSavepoint

        Object result = proceedExecution(method, this.connection, args, this.proxyConfig.getListeners(), this.connectionInfo, null, onComplete);

        if ("createBatch".equals(methodName)) {
            return this.proxyConfig.getProxyFactory().createBatch((Batch) result, this.connectionInfo);
        } else if ("createStatement".equals(methodName)) {
            String query = (String) args[0];
            return this.proxyConfig.getProxyFactory().createStatement((Statement) result, query, this.connectionInfo);
        }

        return result;
    }

}
