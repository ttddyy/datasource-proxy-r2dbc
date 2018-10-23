package net.ttddyy.dsproxy.r2dbc;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 *
 * @author Tadaya Tsuyukubo
 */
public class ProxyConnectionFactory implements ConnectionFactory {

    private ConnectionFactory delegate;

    private ProxyConfig proxyConfig;

    public ProxyConnectionFactory(ConnectionFactory delegate, ProxyConfig proxyConfig) {
        this.delegate = delegate;
        this.proxyConfig = proxyConfig;
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
}
