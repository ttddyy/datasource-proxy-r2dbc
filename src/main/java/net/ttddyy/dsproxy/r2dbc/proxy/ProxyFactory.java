package net.ttddyy.dsproxy.r2dbc.proxy;

import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Statement;
import net.ttddyy.dsproxy.r2dbc.core.ConnectionInfo;

/**
 *
 * @author Tadaya Tsuyukubo
 */
public interface ProxyFactory {

    void setProxyConfig(ProxyConfig proxyConfig);

    ConnectionFactory createConnectionFactory(ConnectionFactory connectionFactory);

    Connection createConnection(Connection connection, ConnectionInfo connectionInfo);

    Batch<?> createBatch(Batch<?> batch, ConnectionInfo connectionInfo);

    Statement<?> createStatement(Statement<?> statement, String query, ConnectionInfo connectionInfo);

}
