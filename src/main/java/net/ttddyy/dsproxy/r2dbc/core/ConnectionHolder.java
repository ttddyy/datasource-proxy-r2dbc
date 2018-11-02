package net.ttddyy.dsproxy.r2dbc.core;

import io.r2dbc.spi.Connection;

/**
 * Provide methods to retrieve {@link Connection} from proxy object.
 *
 * {@link Connection}, {@link io.r2dbc.spi.Batch}, and {@link io.r2dbc.spi.Statement} created
 * by {@link net.ttddyy.dsproxy.r2dbc.ProxyFactory} implement this interface.
 *
 * @author Tadaya Tsuyukubo
 * @see Connection
 * @see io.r2dbc.spi.Batch
 * @see io.r2dbc.spi.Statement
 */
public interface ConnectionHolder {

    Connection getOriginalConnection();

}
