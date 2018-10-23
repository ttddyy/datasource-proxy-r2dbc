package net.ttddyy.dsproxy.r2dbc;

import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import net.ttddyy.dsproxy.r2dbc.core.Bindings;
import net.ttddyy.dsproxy.r2dbc.core.ExecutionInfo;
import net.ttddyy.dsproxy.r2dbc.core.ExecutionType;
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
    private ProxyConfig proxyConfig;

    private String connectionId;
    private String query;

    private List<Bindings> bindings = new ArrayList<>();
    private int currentBindingsIndex = 0;

    public ReactiveStatementCallback(Statement<?> statement, String query, String connectionId, ProxyConfig proxyConfig) {
        this.statement = statement;
        this.query = query;
        this.connectionId = connectionId;
        this.proxyConfig = proxyConfig;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        String methodName = method.getName();
        Object result = proceedExecution(method, this.statement, args);

        // add, bind, bindNull, execute
        if ("add".equals(methodName)) {
            this.currentBindingsIndex++;
        } else if ("bind".equals(methodName)) {

            if (this.bindings.size() <= this.currentBindingsIndex) {
                this.bindings.add(new Bindings());
            }
            Bindings bindings = this.bindings.get(this.currentBindingsIndex);

            if (args[0] instanceof Integer) {
                bindings.addIndexBinding((int) args[0], args[1]);
            } else {
                bindings.addIdentifierBinding(args[0], args[1]);
            }
        } else if ("bindNull".equals(methodName)) {
            // TODO: impl
        } else if ("execute".equals(methodName)) {

            // build ExecutionInfo  TODO: improve
            QueryInfo queryInfo = new QueryInfo(this.query);
            queryInfo.getBindingsList().addAll(this.bindings);
            List<QueryInfo> queries = Stream.of(queryInfo).collect(toList());

            ExecutionInfo execInfo = new ExecutionInfo();
            execInfo.setType(ExecutionType.STATEMENT);
            execInfo.setQueries(queries);
            execInfo.setBindingsSize(this.bindings.size());
            execInfo.setMethod(method);
            execInfo.setMethodArgs(args);
            execInfo.setConnectionId(this.connectionId);

            // API defines "execute()" returns a publisher
            Publisher<? extends Result> publisher = (Publisher<? extends Result>) result;

            return interceptQueryExecution(publisher, proxyConfig.getListeners(), execInfo);
        }

        return proceedExecution(method, this.statement, args);
    }

}
