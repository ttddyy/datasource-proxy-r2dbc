package net.ttddyy.dsproxy.r2dbc.proxy;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import net.ttddyy.dsproxy.r2dbc.core.ConnectionInfo;
import net.ttddyy.dsproxy.r2dbc.core.MethodExecutionInfo;

import java.lang.reflect.Method;
import java.util.function.BiFunction;

/**
 * Proxy callback for {@link ConnectionFactory}.
 *
 * @author Tadaya Tsuyukubo
 */
public class ReactiveConnectionFactoryCallback extends CallbackSupport {

    private ConnectionFactory connectionFactory;

    public ReactiveConnectionFactoryCallback(ConnectionFactory connectionFactory, ProxyConfig proxyConfig) {
        super(proxyConfig);
        this.connectionFactory = connectionFactory;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        String methodName = method.getName();

        if ("getTarget".equals(methodName)) {
            return this.connectionFactory;
        }

        BiFunction<Object, MethodExecutionInfo, Object> onMap = null;
        if ("create".equals(methodName)) {

            // callback for creating connection proxy
            onMap = (resultObj, executionInfo) -> {
                executionInfo.setResult(resultObj);

                Connection connection = (Connection) resultObj;  // original connection
                String connectionId = this.proxyConfig.getConnectionIdManager().getId(connection);

                ConnectionInfo connectionInfo = new ConnectionInfo();
                connectionInfo.setConnectionId(connectionId);
                connectionInfo.setClosed(false);
                connectionInfo.setOriginalConnection(connection);

                executionInfo.setConnectionInfo(connectionInfo);

                Connection proxyConnection = proxyConfig.getProxyFactory().createConnection(connection, connectionInfo);

                return proxyConnection;
            };

        }

        Object result = proceedExecution(method, this.connectionFactory, args, this.proxyConfig.getListeners(), null, onMap, null);
        return result;
    }

}
