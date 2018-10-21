package net.ttddyy.dsproxy.r2dbc.core;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Copy from datasource-proxy
 *
 * removed some of the attributes.
 *
 * @author Tadaya Tsuyukubo
 */
public class ExecutionInfo {

    private String dataSourceName;
    private String connectionId;
    private Method method;
    private Object[] methodArgs;
    private Object result;
    private long elapsedTime;
    private Throwable throwable;
    private boolean isSuccess;
    private int batchSize;  // num of Batch#add

    private ExecutionType type;
    private int statementSize;  // num of Statement#add

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

    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    /**
     * Contains query execution result.
     * Only available after successful query execution.
     *
     * @return result of query
     */
    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    /**
     * Duration of query execution.
     * Only available after successful query execution.
     *
     * @return query execution time
     */
    public long getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(long elapsedTime) {
        this.elapsedTime = elapsedTime;
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

    public int getStatementSize() {
        return statementSize;
    }

    public void setStatementSize(int statementSize) {
        this.statementSize = statementSize;
    }
}
