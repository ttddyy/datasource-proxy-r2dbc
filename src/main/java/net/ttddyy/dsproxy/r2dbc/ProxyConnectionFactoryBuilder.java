package net.ttddyy.dsproxy.r2dbc;

import io.r2dbc.spi.ConnectionFactory;
import net.ttddyy.dsproxy.r2dbc.core.MethodExecutionInfo;
import net.ttddyy.dsproxy.r2dbc.core.ProxyExecutionListener;
import net.ttddyy.dsproxy.r2dbc.core.QueryExecutionInfo;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author Tadaya Tsuyukubo
 */
public class ProxyConnectionFactoryBuilder {

    private ConnectionFactory delegate;

    private ProxyConfig proxyConfig = new ProxyConfig(); // default

    public ProxyConnectionFactoryBuilder(ConnectionFactory delegate) {
        this.delegate = delegate;
    }

    public static ProxyConnectionFactoryBuilder create(ConnectionFactory delegate) {
        Objects.requireNonNull(delegate, "ConnectionFactory to delegate is required");
        return new ProxyConnectionFactoryBuilder(delegate);
    }

    public static ProxyConnectionFactoryBuilder create(ConnectionFactory delegate, ProxyConfig proxyConfig) {
        return create(delegate).proxyConfig(proxyConfig);
    }

    public ConnectionFactory build() {
        return this.proxyConfig.getProxyFactory().createConnectionFactory(this.delegate);
    }


    public ProxyConnectionFactoryBuilder proxyConfig(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
        return this;
    }

    public ProxyConnectionFactoryBuilder onMethodExecution(Consumer<Mono<MethodExecutionInfo>> consumer) {
        this.proxyConfig.addListener(new ProxyExecutionListener() {
            @Override
            public void onMethodExecution(MethodExecutionInfo executionInfo) {
                consumer.accept(Mono.just(executionInfo));
            }
        });
        return this;
    }

    public ProxyConnectionFactoryBuilder onQueryExecution(Consumer<Mono<QueryExecutionInfo>> consumer) {
        this.proxyConfig.addListener(new ProxyExecutionListener() {
            @Override
            public void onQueryExecution(QueryExecutionInfo executionInfo) {
                consumer.accept(Mono.just(executionInfo));
            }
        });
        return this;
    }

    public ProxyConnectionFactoryBuilder onBeforeMethod(Consumer<Mono<MethodExecutionInfo>> consumer) {
        this.proxyConfig.addListener(new ProxyExecutionListener() {
            @Override
            public void beforeMethod(MethodExecutionInfo executionInfo) {
                consumer.accept(Mono.just(executionInfo));
            }
        });
        return this;
    }

    public ProxyConnectionFactoryBuilder onAfterMethod(Consumer<Mono<MethodExecutionInfo>> consumer) {
        this.proxyConfig.addListener(new ProxyExecutionListener() {
            @Override
            public void afterMethod(MethodExecutionInfo executionInfo) {
                consumer.accept(Mono.just(executionInfo));
            }
        });
        return this;
    }

    public ProxyConnectionFactoryBuilder onBeforeQuery(Consumer<Mono<QueryExecutionInfo>> consumer) {
        this.proxyConfig.addListener(new ProxyExecutionListener() {
            @Override
            public void beforeQuery(QueryExecutionInfo executionInfo) {
                consumer.accept(Mono.just(executionInfo));
            }
        });
        return this;
    }

    public ProxyConnectionFactoryBuilder onAfterQuery(Consumer<Mono<QueryExecutionInfo>> consumer) {
        this.proxyConfig.addListener(new ProxyExecutionListener() {
            @Override
            public void afterQuery(QueryExecutionInfo executionInfo) {
                consumer.accept(Mono.just(executionInfo));
            }
        });
        return this;
    }

    public ProxyConnectionFactoryBuilder listener(ProxyExecutionListener listener) {
        this.proxyConfig.addListener(listener);
        return this;
    }

}
