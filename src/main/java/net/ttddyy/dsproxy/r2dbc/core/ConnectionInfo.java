package net.ttddyy.dsproxy.r2dbc.core;


import io.r2dbc.spi.Connection;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link Connection} related information.
 *
 * @author Tadaya Tsuyukubo
 */
public class ConnectionInfo {

    private Connection originalConnection;
    private String connectionId;
    private AtomicBoolean isClosed = new AtomicBoolean();
    private AtomicInteger transactionCount = new AtomicInteger();
    private AtomicInteger commitCount = new AtomicInteger();
    private AtomicInteger rollbackCount = new AtomicInteger();

    // TODO: may keep transaction isolation level

    public Connection getOriginalConnection() {
        return this.originalConnection;
    }

    public void setOriginalConnection(Connection originalConnection) {
        this.originalConnection = originalConnection;
    }

    public String getConnectionId() {
        return this.connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    /**
     * Increment transaction count.
     */
    public void incrementTransactionCount() {
        this.transactionCount.incrementAndGet();
    }

    /**
     * Increment commit count.
     */
    public void incrementCommitCount() {
        this.commitCount.incrementAndGet();
    }

    /**
     * Increment rollback count.
     */
    public void incrementRollbackCount() {
        this.rollbackCount.incrementAndGet();
    }


    /**
     * Returns how many times {@link Connection#beginTransaction()} method is called.
     *
     * @return num of beginTransaction() method being called
     */
    public int getTransactionCount() {
        return this.transactionCount.get();
    }

    /**
     * Returns how many times {@link Connection#commitTransaction()} method is called.
     *
     * @return num of commitTransaction method being called
     */
    public int getCommitCount() {
        return this.commitCount.get();
    }

    /**
     * Returns how many times {@link Connection#rollbackTransaction()} method is called.
     *
     * @return num of rollback methods being called
     */
    public int getRollbackCount() {
        return this.rollbackCount.get();
    }

    /**
     */
    public boolean isClosed() {
        return this.isClosed.get();
    }

    /**
     */
    public void setClosed(boolean closed) {
        this.isClosed.set(closed);
    }

}
