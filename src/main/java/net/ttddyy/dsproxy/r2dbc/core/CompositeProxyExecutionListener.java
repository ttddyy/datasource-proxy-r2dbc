package net.ttddyy.dsproxy.r2dbc.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Delegate to multiple of {@link ProxyExecutionListener ProxyExecutionListeners}.
 *
 * @author Tadaya Tsuyukubo
 */
public class CompositeProxyExecutionListener implements ProxyExecutionListener {
    private List<ProxyExecutionListener> listeners = new ArrayList<>();

    public CompositeProxyExecutionListener(ProxyExecutionListener... listeners) {
        this.listeners.addAll(Arrays.asList(listeners));
    }

    @Override
    public void onMethodExecution(MethodExecutionInfo executionInfo) {
        this.listeners.forEach(listener -> listener.onMethodExecution(executionInfo));
    }

    @Override
    public void onQueryExecution(QueryExecutionInfo executionInfo) {
        this.listeners.forEach(listener -> listener.onQueryExecution(executionInfo));
    }

    @Override
    public void beforeMethod(MethodExecutionInfo executionInfo) {
        this.listeners.forEach(listener -> listener.beforeMethod(executionInfo));
    }

    @Override
    public void afterMethod(MethodExecutionInfo executionInfo) {
        this.listeners.forEach(listener -> listener.afterMethod(executionInfo));
    }

    @Override
    public void beforeQuery(QueryExecutionInfo execInfo) {
        this.listeners.forEach(listener -> listener.beforeQuery(execInfo));
    }

    @Override
    public void afterQuery(QueryExecutionInfo execInfo) {
        this.listeners.forEach(listener -> listener.afterQuery(execInfo));
    }

    @Override
    public void eachQueryResult(QueryExecutionInfo execInfo) {
        this.listeners.forEach(listener -> listener.eachQueryResult(execInfo));
    }

    public boolean add(ProxyExecutionListener listener) {
        return this.listeners.add(listener);
    }

    public boolean addAll(Collection<ProxyExecutionListener> listeners) {
        return this.listeners.addAll(listeners);
    }

    public List<ProxyExecutionListener> getListeners() {
        return this.listeners;
    }

//    public void setListeners(List<ProxyExecutionListener> listeners) {
//        this.listeners = listeners;
//    }

}
