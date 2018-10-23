package net.ttddyy.dsproxy.r2dbc.core;

import io.r2dbc.spi.Connection;

/**
 * @author Tadaya Tsuyukubo
 */
public interface ConnectionIdManager {

    String getId(Connection connection);

}
