package net.ttddyy.dsproxy.r2dbc.core;

import io.r2dbc.spi.Result;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Copy from datasource-proxy
 *
 * removed some of the attributes.
 *
 * @author Tadaya Tsuyukubo
 */
public class QueryExecutionInfo {

    private String connectionId = "";
    private Method method;
    private Object[] methodArgs;
    private Throwable throwable;
    private boolean isSuccess;
    private int batchSize;  // num of Batch#add

    private ExecutionType type;
    private int bindingsSize;  // num of Statement#add
    private Duration executeDuration = Duration.ZERO;
    private String threadName = "";
    private long threadId;
    private ProxyEventType proxyEventType;
    private int currentResultCount;
    private Result currentResult;

    private List<QueryInfo> queries = new ArrayList<>();

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Object[] getMethodArgs() {
        return methodArgs;
    }

    public void setMethodArgs(Object[] methodArgs) {
        this.methodArgs = methodArgs;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * Contains an exception thrown while query was executed.
     * Contains value only when an exception has thrown, otherwise {@code null}.
     *
     * @param throwable an error thrown while executing a query
     */
    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    /**
     * Indicate whether the query execution was successful or not.
     * Contains valid value only after the query execution.
     *
     * @return true when query has successfully executed
     */
    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * Returns list of {@link QueryInfo}.
     *
     * @return list of queries. This will NOT return null.
     */
    public List<QueryInfo> getQueries() {
        return this.queries;
    }

    public void setQueries(List<QueryInfo> queries) {
        this.queries = queries;
    }

    public ExecutionType getType() {
        return type;
    }

    public void setType(ExecutionType type) {
        this.type = type;
    }

    public int getBindingsSize() {
        return bindingsSize;
    }

    public void setBindingsSize(int bindingsSize) {
        this.bindingsSize = bindingsSize;
    }

    public Duration getExecuteDuration() {
        return executeDuration;
    }

    public void setExecuteDuration(Duration executeDuration) {
        this.executeDuration = executeDuration;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    public ProxyEventType getProxyEventType() {
        return proxyEventType;
    }

    public void setProxyEventType(ProxyEventType proxyEventType) {
        this.proxyEventType = proxyEventType;
    }

    /**
     * Represent Nth {@link io.r2dbc.spi.Result}.
     *
     * On each query result callback({@link ProxyExecutionListener#eachQueryResult(QueryExecutionInfo)}),
     * this value indicates Nth {@link Result} starting from 1.
     * (1st query result, 2nd query result, 3rd, 4th,...).
     *
     * This returns 0 for before query execution({@link ProxyExecutionListener#beforeQuery(QueryExecutionInfo)}).
     * For after query execution({@link ProxyExecutionListener#afterQuery(QueryExecutionInfo)}), this returns
     * total number of {@link io.r2dbc.spi.Result} returned by this query execution.
     *
     * @return Nth number of query result
     */
    public int getCurrentResultCount() {
        return currentResultCount;
    }

    public void setCurrentResultCount(int currentResultCount) {
        this.currentResultCount = currentResultCount;
    }

    /**
     * Current query {@link Result} available for each-query-result-callback({@link ProxyExecutionListener#eachQueryResult(QueryExecutionInfo)}).
     *
     * For before and after query execution({@link ProxyExecutionListener#beforeQuery(QueryExecutionInfo)}
     * and {@link ProxyExecutionListener#afterQuery(QueryExecutionInfo)), this returns {@code null}.
     *
     * @return
     */
    public Result getCurrentResult() {
        return currentResult;
    }

    public void setCurrentResult(Result currentResult) {
        this.currentResult = currentResult;
    }
}
