package net.ttddyy.dsproxy.r2dbc.core;

/**
 * Keep the last invoked execution.
 *
 * Used for validating last execution.
 *
 * @author Tadaya Tsuyukubo
 */
public class LastExecutionAwareListener implements ProxyDataSourceListener {

    private QueryExecutionInfo beforeQueryExecutionInfo;
    private QueryExecutionInfo afterQueryExecutionInfo;
    private MethodExecutionInfo beforeMethodExecutionInfo;
    private MethodExecutionInfo afterMethodExecutionInfo;

    @Override
    public void beforeQuery(QueryExecutionInfo execInfo) {
        this.beforeQueryExecutionInfo = execInfo;
    }

    @Override
    public void afterQuery(QueryExecutionInfo execInfo) {
        this.afterQueryExecutionInfo = execInfo;
    }

    @Override
    public void beforeMethod(MethodExecutionInfo executionInfo) {
        this.beforeMethodExecutionInfo = executionInfo;
    }

    @Override
    public void afterMethod(MethodExecutionInfo executionInfo) {
        this.afterMethodExecutionInfo = executionInfo;
    }

    public QueryExecutionInfo getBeforeQueryExecutionInfo() {
        return beforeQueryExecutionInfo;
    }

    public QueryExecutionInfo getAfterQueryExecutionInfo() {
        return afterQueryExecutionInfo;
    }

    public MethodExecutionInfo getBeforeMethodExecutionInfo() {
        return beforeMethodExecutionInfo;
    }

    public MethodExecutionInfo getAfterMethodExecutionInfo() {
        return afterMethodExecutionInfo;
    }
}
