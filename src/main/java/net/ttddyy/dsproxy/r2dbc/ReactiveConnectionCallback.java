package net.ttddyy.dsproxy.r2dbc;

import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Statement;
import net.ttddyy.dsproxy.r2dbc.core.ConnectionInfo;

import java.lang.reflect.Method;

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

        Object result = proceedExecution(method, this.connection, args, this.proxyConfig.getListeners(), this.connectionInfo);

        if ("createBatch".equals(methodName)) {
            return this.proxyConfig.getProxyFactory().createBatch((Batch) result, this.connectionInfo);
        } else if ("createStatement".equals(methodName)) {
            String query = (String) args[0];
            return this.proxyConfig.getProxyFactory().createStatement((Statement) result, query, this.connectionInfo);
        }

        return result;
    }

}
