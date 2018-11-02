package net.ttddyy.dsproxy.r2dbc.core;

import io.r2dbc.spi.Connection;
import net.ttddyy.dsproxy.r2dbc.JdkProxyFactory;
import net.ttddyy.dsproxy.r2dbc.ProxyConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

/**
 * @author Tadaya Tsuyukubo
 */
public class ConnectionInfoTest {

    @Test
    void getOriginalConnection() {

        Connection originalConnection = mock(Connection.class);

        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setProxyFactory(new JdkProxyFactory());

        ConnectionInfo connectionInfo = new ConnectionInfo();

        Connection proxyConnection = proxyConfig.getProxyFactory().createConnection(originalConnection, connectionInfo);

        // this set is internal operation to associate created proxy to connectionInfo
        connectionInfo.setProxyConnection(proxyConnection);

        assertSame(originalConnection, connectionInfo.getOriginalConnection());

    }

}
