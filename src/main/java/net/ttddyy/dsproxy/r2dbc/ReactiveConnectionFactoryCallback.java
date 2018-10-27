package net.ttddyy.dsproxy.r2dbc;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.lang.reflect.Method;

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

        Object result = proceedExecution(method, this.connectionFactory, args, this.proxyConfig.getListeners(), null);
        if ("create".equals(methodName)) {

            // Since "proceedExecution()" has specially handled "ConnectionFactory#create", unwrap
            // the returned Tuple2, and return proxied connection.
            return Mono.empty()
                    .concatWith((Publisher) result)
                    .map(resultTuple -> {
                        Tuple2 t2 = ((Tuple2) resultTuple);
                        Connection connection = (Connection) t2.getT1();
                        String connectionId = (String) t2.getT2();
                        return proxyConfig.getProxyFactory().createConnection(connection, connectionId);
                    });
        }

        return result;
    }

}
