package net.ttddyy.dsproxy.r2dbc;

import net.ttddyy.dsproxy.r2dbc.core.ProxyDataSourceListener;
import net.ttddyy.dsproxy.r2dbc.core.QueryExecutionInfo;

/**
 * @author Tadaya Tsuyukubo
 */
public class TestProxyDataSourceListener implements ProxyDataSourceListener {

    private QueryExecutionInfo beforeQueryExecutionInfo;
    private QueryExecutionInfo afterQueryExecutionInfo;

    @Override
    public void beforeQuery(QueryExecutionInfo execInfo) {
        this.beforeQueryExecutionInfo = execInfo;
    }

    @Override
    public void afterQuery(QueryExecutionInfo execInfo) {
        this.afterQueryExecutionInfo = execInfo;
    }

    public QueryExecutionInfo getBeforeQueryExecutionInfo() {
        return beforeQueryExecutionInfo;
    }

    public QueryExecutionInfo getAfterQueryExecutionInfo() {
        return afterQueryExecutionInfo;
    }
}
