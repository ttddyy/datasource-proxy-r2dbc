package net.ttddyy.dsproxy.r2dbc.proxy;

import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import net.ttddyy.dsproxy.r2dbc.core.BindingValue;
import net.ttddyy.dsproxy.r2dbc.core.BindingValue.NullBindingValue;
import net.ttddyy.dsproxy.r2dbc.core.BindingValue.SimpleBindingValue;
import net.ttddyy.dsproxy.r2dbc.core.Bindings;
import net.ttddyy.dsproxy.r2dbc.core.ConnectionInfo;
import net.ttddyy.dsproxy.r2dbc.core.ExecutionType;
import net.ttddyy.dsproxy.r2dbc.core.QueryExecutionInfo;
import net.ttddyy.dsproxy.r2dbc.core.QueryInfo;
import org.reactivestreams.Publisher;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Proxy callback for {@link Statement}.
 *
 * @author Tadaya Tsuyukubo
 */
public class ReactiveStatementCallback extends CallbackSupport {

    private Statement<?> statement;

    private ConnectionInfo connectionInfo;
    private String query;

    private List<Bindings> bindings = new ArrayList<>();
    private int currentBindingsIndex = 0;

    public ReactiveStatementCallback(Statement<?> statement, String query, ConnectionInfo connectionInfo, ProxyConfig proxyConfig) {
        super(proxyConfig);
        this.statement = statement;
        this.query = query;
        this.connectionInfo = connectionInfo;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        String methodName = method.getName();

        if ("getTarget".equals(methodName)) {
            return this.statement;
        } else if ("getOriginalConnection".equals(methodName)) {
            return this.connectionInfo.getOriginalConnection();
        }

        Object result = proceedExecution(method, this.statement, args, this.proxyConfig.getListeners(), this.connectionInfo, null, null);

        // add, bind, bindNull, execute
        if ("add".equals(methodName)) {
            this.currentBindingsIndex++;
        } else if ("bind".equals(methodName) || "bindNull".equals(methodName)) {

            if (this.bindings.size() <= this.currentBindingsIndex) {
                this.bindings.add(new Bindings());
            }
            Bindings bindings = this.bindings.get(this.currentBindingsIndex);

            BindingValue bindingValue;
            if ("bind".equals(methodName)) {
                bindingValue = new SimpleBindingValue(args[1]);
            } else {
                bindingValue = new NullBindingValue((Class<?>) args[1]);
            }

            if (args[0] instanceof Integer) {
                bindings.addIndexBinding((int) args[0], bindingValue);
            } else {
                bindings.addIdentifierBinding(args[0], bindingValue);
            }
        } else if ("execute".equals(methodName)) {

            // build QueryExecutionInfo  TODO: improve
            QueryInfo queryInfo = new QueryInfo(this.query);
            queryInfo.getBindingsList().addAll(this.bindings);
            List<QueryInfo> queries = Stream.of(queryInfo).collect(toList());

            QueryExecutionInfo execInfo = new QueryExecutionInfo();
            execInfo.setType(ExecutionType.STATEMENT);
            execInfo.setQueries(queries);
            execInfo.setBindingsSize(this.bindings.size());
            execInfo.setMethod(method);
            execInfo.setMethodArgs(args);
            execInfo.setConnectionInfo(this.connectionInfo);

            // API defines "execute()" returns a publisher
            Publisher<? extends Result> publisher = (Publisher<? extends Result>) result;

            return interceptQueryExecution(publisher, execInfo);
        }

        return result;
    }

}
