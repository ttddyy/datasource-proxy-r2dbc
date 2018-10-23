package net.ttddyy.dsproxy.r2dbc;

import net.ttddyy.dsproxy.r2dbc.core.ExecutionInfo;
import net.ttddyy.dsproxy.r2dbc.core.ProxyDataSourceListener;

/**
 * @author Tadaya Tsuyukubo
 */
public class TestProxyDataSourceListener implements ProxyDataSourceListener {

    private ExecutionInfo beforeQueryExecutionInfo;
    private ExecutionInfo afterQueryExecutionInfo;

    @Override
    public void beforeQuery(ExecutionInfo execInfo) {
        this.beforeQueryExecutionInfo = execInfo;
    }

    @Override
    public void afterQuery(ExecutionInfo execInfo) {
        this.afterQueryExecutionInfo = execInfo;
    }

    public ExecutionInfo getBeforeQueryExecutionInfo() {
        return beforeQueryExecutionInfo;
    }

    public ExecutionInfo getAfterQueryExecutionInfo() {
        return afterQueryExecutionInfo;
    }
}
