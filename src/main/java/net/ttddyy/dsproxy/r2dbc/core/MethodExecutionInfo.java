package net.ttddyy.dsproxy.r2dbc.core;

import java.lang.reflect.Method;
import java.time.Duration;

/**
 * Copy of MethodExecutionContext from datasource-proxy and some modification.
 *
 * @author Tadaya Tsuyukubo
 */
public class MethodExecutionInfo {

    private Object target;
    private Method method;
    private Object[] methodArgs;
    private Object result;
    private Throwable thrown;
    private String connectionId;

    private Duration executeDuration = Duration.ZERO;
    private String threadName;
    private long threadId;
    private ProxyEventType proxyEventType;


    public Object getTarget() {
        return target;
    }

    public void setTarget(Object target) {
        this.target = target;
    }

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

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Throwable getThrown() {
        return thrown;
    }

    public void setThrown(Throwable thrown) {
        this.thrown = thrown;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
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
}
