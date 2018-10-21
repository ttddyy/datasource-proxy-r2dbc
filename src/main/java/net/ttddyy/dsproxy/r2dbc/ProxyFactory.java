package net.ttddyy.dsproxy.r2dbc;

import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Statement;

/**
 *
 * @author Tadaya Tsuyukubo
 */
public interface ProxyFactory {

    void setProxyConfig(ProxyConfig proxyConfig);

    Connection createConnection(Connection connection);

    Batch<?> createBatch(Batch<?> batch);

    Statement<?> createStatement(Statement<?> statement, String query);

}
