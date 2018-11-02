package net.ttddyy.dsproxy.r2dbc;

import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Statement;
import net.ttddyy.dsproxy.r2dbc.core.ConnectionHolder;
import net.ttddyy.dsproxy.r2dbc.core.ConnectionInfo;
import net.ttddyy.dsproxy.r2dbc.core.ProxyObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Tadaya Tsuyukubo
 */
public class JdkProxyFactoryTest {

    private JdkProxyFactory proxyFactory;

    @BeforeEach
    void setUp() {
        this.proxyFactory = new JdkProxyFactory();
        this.proxyFactory.setProxyConfig(new ProxyConfig());
    }

    @Test
    void isProxy() {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        Connection connection = mock(Connection.class);
        Batch batch = mock(Batch.class);
        Statement statement = mock(Statement.class);
        ConnectionInfo connectionInfo = new ConnectionInfo();
        String query = "query";

        Object result;

        result = this.proxyFactory.createConnectionFactory(connectionFactory);
        assertTrue(Proxy.isProxyClass(result.getClass()));
        assertThat(result).isInstanceOf(ProxyObject.class);
        assertThat(result).isNotInstanceOf(ConnectionHolder.class);

        result = this.proxyFactory.createConnection(connection, connectionInfo);
        assertTrue(Proxy.isProxyClass(result.getClass()));
        assertThat(result).isInstanceOf(ProxyObject.class);
        assertThat(result).isInstanceOf(ConnectionHolder.class);

        result = this.proxyFactory.createBatch(batch, connectionInfo);
        assertTrue(Proxy.isProxyClass(result.getClass()));
        assertThat(result).isInstanceOf(ProxyObject.class);
        assertThat(result).isInstanceOf(ConnectionHolder.class);

        result = this.proxyFactory.createStatement(statement, query, connectionInfo);
        assertTrue(Proxy.isProxyClass(result.getClass()));
        assertThat(result).isInstanceOf(ProxyObject.class);
        assertThat(result).isInstanceOf(ConnectionHolder.class);
    }

    @Test
    void testToString() {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        Connection connection = mock(Connection.class);
        Batch batch = mock(Batch.class);
        Statement statement = mock(Statement.class);
        ConnectionInfo connectionInfo = new ConnectionInfo();
        String query = "query";

        String expected;
        Object result;

        result = this.proxyFactory.createConnectionFactory(connectionFactory);
        expected = getExpectedToString(connectionFactory);
        assertEquals(expected, result.toString());

        result = this.proxyFactory.createConnection(connection, connectionInfo);
        expected = getExpectedToString(connection);
        assertEquals(expected, result.toString());

        result = this.proxyFactory.createBatch(batch, connectionInfo);
        expected = getExpectedToString(batch);
        assertEquals(expected, result.toString());

        result = this.proxyFactory.createStatement(statement, query, connectionInfo);
        expected = getExpectedToString(statement);
        assertEquals(expected, result.toString());

    }

    private String getExpectedToString(Object target) {
        return target.getClass().getSimpleName() + "-proxy [" + target.toString() + "]";
    }
}
