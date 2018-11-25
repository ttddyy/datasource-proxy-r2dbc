package net.ttddyy.dsproxy.r2dbc.proxy;

import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import net.ttddyy.dsproxy.r2dbc.core.ConnectionInfo;
import net.ttddyy.dsproxy.r2dbc.core.QueryExecutionInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

/**
 * @author Tadaya Tsuyukubo
 */
public class ProxyUtilsTest {

    @Test
    void getOriginalConnection() {
        Connection originalConnection = mock(Connection.class);
        Batch originalBatch = mock(Batch.class);
        Statement originalStatement = mock(Statement.class);
        Result originalResult = mock(Result.class);

        String query = "QUERY";

        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setProxyFactory(new JdkProxyFactory());

        ConnectionInfo connectionInfo = new ConnectionInfo();

        Connection proxyConnection = proxyConfig.getProxyFactory().createConnection(originalConnection, connectionInfo);
        connectionInfo.setOriginalConnection(originalConnection);

        QueryExecutionInfo queryExecutionInfo = new QueryExecutionInfo();
        queryExecutionInfo.setConnectionInfo(connectionInfo);

        Batch proxyBatch = proxyConfig.getProxyFactory().createBatch(originalBatch, connectionInfo);
        Statement proxyStatement = proxyConfig.getProxyFactory().createStatement(originalStatement, query, connectionInfo);
        Result proxyResult = proxyConfig.getProxyFactory().createResult(originalResult, queryExecutionInfo);

        Connection result;

        result = ProxyUtils.getOriginalConnection(proxyConnection);
        assertSame(originalConnection, result);

        result = ProxyUtils.getOriginalConnection(proxyBatch);
        assertSame(originalConnection, result);

        result = ProxyUtils.getOriginalConnection(proxyStatement);
        assertSame(originalConnection, result);

        result = ProxyUtils.getOriginalConnection(proxyResult);
        assertSame(originalConnection, result);
    }

}
