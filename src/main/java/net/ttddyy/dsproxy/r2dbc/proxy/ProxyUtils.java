package net.ttddyy.dsproxy.r2dbc.proxy;

import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Statement;
import net.ttddyy.dsproxy.r2dbc.core.ConnectionHolder;
import net.ttddyy.dsproxy.r2dbc.proxy.ProxyObject;

/**
 * @author Tadaya Tsuyukubo
 */
public class ProxyUtils {

    private ProxyUtils() {
    }

    public static Connection getOriginalConnection(Connection connection) {
        if (connection instanceof ProxyObject) {
            return (Connection) ((ProxyObject) connection).getTarget();
        }
        return connection;
    }

    public static Connection getOriginalConnection(Batch<?> batch) {
        if (batch instanceof ConnectionHolder) {
            return ((ConnectionHolder) batch).getOriginalConnection();
        }
        return null;
    }

    public static Connection getOriginalConnection(Statement<?> statement) {
        if (statement instanceof ConnectionHolder) {
            return ((ConnectionHolder) statement).getOriginalConnection();
        }
        return null;
    }


}
