package net.ttddyy.dsproxy.r2dbc.proxy;

import net.ttddyy.dsproxy.r2dbc.core.CompositeProxyExecutionListener;
import net.ttddyy.dsproxy.r2dbc.core.ConnectionIdManager;
import net.ttddyy.dsproxy.r2dbc.core.DefaultConnectionIdManager;
import net.ttddyy.dsproxy.r2dbc.core.ProxyExecutionListener;

/**
 *
 * @author Tadaya Tsuyukubo
 */
public class ProxyConfig {

    private CompositeProxyExecutionListener listeners = new CompositeProxyExecutionListener();

    private ConnectionIdManager connectionIdManager = new DefaultConnectionIdManager();

    private ProxyFactory proxyFactory = new JdkProxyFactory();

    {
        this.proxyFactory.setProxyConfig(this);
    }

    //    public ProxyConfig(ProxyFactory proxyFactory) {
    //        this.proxyFactory = proxyFactory;
    //    }

    public ProxyFactory getProxyFactory() {
        return proxyFactory;
    }

    public void setProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
        this.proxyFactory.setProxyConfig(this);
    }

    public CompositeProxyExecutionListener getListeners() {
        return this.listeners;
    }

    public void addListener(ProxyExecutionListener listener) {
        this.listeners.add(listener);
    }

    public ConnectionIdManager getConnectionIdManager() {
        return connectionIdManager;
    }

    public void setConnectionIdManager(ConnectionIdManager connectionIdManager) {
        this.connectionIdManager = connectionIdManager;
    }
}
