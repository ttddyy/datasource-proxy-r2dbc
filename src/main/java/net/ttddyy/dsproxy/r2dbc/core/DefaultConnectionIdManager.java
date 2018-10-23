package net.ttddyy.dsproxy.r2dbc.core;

import io.r2dbc.spi.Connection;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Copy from datasource-proxy
 *
 * TODO: manage open connection ids
 *
 * @author Tadaya Tsuyukubo
 */
public class DefaultConnectionIdManager implements ConnectionIdManager {

    private AtomicLong idCounter = new AtomicLong(0);

    @Override
    public String getId(Connection connection) {
        String id = String.valueOf(this.idCounter.incrementAndGet());
        return id;
    }

}
