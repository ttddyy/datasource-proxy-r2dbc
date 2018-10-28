package net.ttddyy.dsproxy.r2dbc;

import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

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
        String connectionId = "foo";
        String query = "query";

        Object result;

        result = this.proxyFactory.createConnectionFactory(connectionFactory);
        assertTrue(Proxy.isProxyClass(result.getClass()));

        result = this.proxyFactory.createConnection(connection, connectionId);
        assertTrue(Proxy.isProxyClass(result.getClass()));

        result = this.proxyFactory.createBatch(batch, connectionId);
        assertTrue(Proxy.isProxyClass(result.getClass()));

        result = this.proxyFactory.createStatement(statement, query, connectionId);
        assertTrue(Proxy.isProxyClass(result.getClass()));
    }

    @Test
    void testToString() {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        Connection connection = mock(Connection.class);
        Batch batch = mock(Batch.class);
        Statement statement = mock(Statement.class);
        String connectionId = "foo";
        String query = "query";

        String expected;
        Object result;

        result = this.proxyFactory.createConnectionFactory(connectionFactory);
        expected = getExpectedToString(connectionFactory);
        assertEquals(expected, result.toString());

        result = this.proxyFactory.createConnection(connection, connectionId);
        expected = getExpectedToString(connection);
        assertEquals(expected, result.toString());

        result = this.proxyFactory.createBatch(batch, connectionId);
        expected = getExpectedToString(batch);
        assertEquals(expected, result.toString());

        result = this.proxyFactory.createStatement(statement, query, connectionId);
        expected = getExpectedToString(statement);
        assertEquals(expected, result.toString());

    }

    private String getExpectedToString(Object target) {
        return target.getClass().getSimpleName() + "-proxy [" + target.toString() + "]";
    }
}
