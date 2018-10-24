package net.ttddyy.dsproxy.r2dbc.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Copy from datasource-proxy
 * @author Tadaya Tsuyukubo
 */
public class CompositeProxyDataSourceListener implements ProxyDataSourceListener {
    private List<ProxyDataSourceListener> listeners = new ArrayList<>();

    @Override
    public void beforeMethod(MethodExecutionInfo executionInfo) {
        this.listeners.forEach(listener -> listener.beforeMethod(executionInfo));
    }

    @Override
    public void afterMethod(MethodExecutionInfo executionInfo) {
        this.listeners.forEach(listener -> listener.afterMethod(executionInfo));
    }

    @Override
    public void beforeQuery(ExecutionInfo execInfo) {
        this.listeners.forEach(listener -> listener.beforeQuery(execInfo));
    }

    @Override
    public void afterQuery(ExecutionInfo execInfo) {
        this.listeners.forEach(listener -> listener.afterQuery(execInfo));
    }

    public boolean add(ProxyDataSourceListener listener) {
        return this.listeners.add(listener);
    }

    public boolean addAll(Collection<ProxyDataSourceListener> listeners) {
        return this.listeners.addAll(listeners);
    }

    public List<ProxyDataSourceListener> getListeners() {
        return this.listeners;
    }

//    public void setListeners(List<ProxyDataSourceListener> listeners) {
//        this.listeners = listeners;
//    }

}
