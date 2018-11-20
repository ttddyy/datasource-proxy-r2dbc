package net.ttddyy.dsproxy.r2dbc.support;

import net.ttddyy.dsproxy.r2dbc.core.MethodExecutionInfo;
import net.ttddyy.dsproxy.r2dbc.core.ProxyExecutionListener;
import net.ttddyy.dsproxy.r2dbc.core.QueryExecutionInfo;

/**
 * Keep the last invoked execution.
 *
 * Used for validating last execution.
 *
 * @author Tadaya Tsuyukubo
 */
public class LastExecutionAwareListener implements ProxyExecutionListener {

    private QueryExecutionInfo beforeQueryExecutionInfo;
    private QueryExecutionInfo afterQueryExecutionInfo;
    private QueryExecutionInfo eachQueryResultExecutionInfo;
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
    public void eachQueryResult(QueryExecutionInfo execInfo) {
        this.eachQueryResultExecutionInfo = execInfo;
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

    public QueryExecutionInfo getEachQueryResultExecutionInfo() {
        return eachQueryResultExecutionInfo;
    }

    public MethodExecutionInfo getBeforeMethodExecutionInfo() {
        return beforeMethodExecutionInfo;
    }

    public MethodExecutionInfo getAfterMethodExecutionInfo() {
        return afterMethodExecutionInfo;
    }
}
