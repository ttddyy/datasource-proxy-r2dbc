package net.ttddyy.dsproxy.r2dbc;

import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Statement;

import java.lang.reflect.Method;

/**
 * Proxy callback for {@link Connection}.
 *
 * @author Tadaya Tsuyukubo
 */
public class ReactiveConnectionCallback extends CallbackSupport {

    private Connection connection;
    private String connectionId;

    public ReactiveConnectionCallback(Connection connection, String connectionId, ProxyConfig proxyConfig) {
        super(proxyConfig);
        this.connection = connection;
        this.connectionId = connectionId;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        String methodName = method.getName();

        Object result = proceedExecution(method, this.connection, args, this.proxyConfig.getListeners(), this.connectionId);

        if ("createBatch".equals(methodName)) {
            return this.proxyConfig.getProxyFactory().createBatch((Batch) result, this.connectionId);
        } else if ("createStatement".equals(methodName)) {
            String query = (String) args[0];
            return this.proxyConfig.getProxyFactory().createStatement((Statement) result, query, this.connectionId);
        }

        return result;
    }

}
