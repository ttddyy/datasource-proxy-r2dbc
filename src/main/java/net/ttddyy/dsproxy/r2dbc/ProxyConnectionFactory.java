package net.ttddyy.dsproxy.r2dbc;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import net.ttddyy.dsproxy.r2dbc.core.MethodExecutionInfo;
import net.ttddyy.dsproxy.r2dbc.core.ProxyExecutionListener;
import net.ttddyy.dsproxy.r2dbc.core.QueryExecutionInfo;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

/**
 * @author Tadaya Tsuyukubo
 */
public class ProxyConnectionFactory implements ConnectionFactory {

    private ConnectionFactory delegate;

    private ProxyConfig proxyConfig = new ProxyConfig(); // default

    public ProxyConnectionFactory(ConnectionFactory delegate) {
        this.delegate = delegate;
    }

    public static ProxyConnectionFactory of(ConnectionFactory delegate) {
        return new ProxyConnectionFactory(delegate);
    }

    public static ProxyConnectionFactory of(ConnectionFactory delegate, ProxyConfig proxyConfig) {
        return of(delegate).proxyConfig(proxyConfig);
    }

    @Override
    public Publisher<? extends Connection> create() {
        Publisher<? extends Connection> pub = this.delegate.create();
        return Mono.from(pub)
                .map(connection -> {
                    String connectionId = this.proxyConfig.getConnectionIdManager().getId(connection);
                    return proxyConfig.getProxyFactory().createConnection(connection, connectionId);
                });
    }

    @Override
    public ConnectionFactoryMetadata getMetadata() {
        return this.delegate.getMetadata();
    }

    public ProxyConnectionFactory proxyConfig(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
        return this;
    }

    public ProxyConnectionFactory onMethod(Consumer<Mono<MethodExecutionInfo>> consumer) {
        this.proxyConfig.addListener(new ProxyExecutionListener() {
            @Override
            public void onMethodExecution(MethodExecutionInfo executionInfo) {
                consumer.accept(Mono.just(executionInfo));
            }
        });
        return this;
    }

    public ProxyConnectionFactory onBeforeMethod(Consumer<Mono<MethodExecutionInfo>> consumer) {
        this.proxyConfig.addListener(new ProxyExecutionListener() {
            @Override
            public void beforeMethod(MethodExecutionInfo executionInfo) {
                consumer.accept(Mono.just(executionInfo));
            }
        });
        return this;
    }

    public ProxyConnectionFactory onAfterMethod(Consumer<Mono<MethodExecutionInfo>> consumer) {
        this.proxyConfig.addListener(new ProxyExecutionListener() {
            @Override
            public void afterMethod(MethodExecutionInfo executionInfo) {
                consumer.accept(Mono.just(executionInfo));
            }
        });
        return this;
    }

    public ProxyConnectionFactory onBeforeQuery(Consumer<Mono<QueryExecutionInfo>> consumer) {
        this.proxyConfig.addListener(new ProxyExecutionListener() {
            @Override
            public void beforeQuery(QueryExecutionInfo executionInfo) {
                consumer.accept(Mono.just(executionInfo));
            }
        });
        return this;
    }

    public ProxyConnectionFactory onAfterQuery(Consumer<Mono<QueryExecutionInfo>> consumer) {
        this.proxyConfig.addListener(new ProxyExecutionListener() {
            @Override
            public void afterQuery(QueryExecutionInfo executionInfo) {
                consumer.accept(Mono.just(executionInfo));
            }
        });
        return this;
    }

}
