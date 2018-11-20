package net.ttddyy.dsproxy.r2dbc;

import io.r2dbc.spi.Statement;
import net.ttddyy.dsproxy.r2dbc.core.Binding;
import net.ttddyy.dsproxy.r2dbc.core.BindingValue;
import net.ttddyy.dsproxy.r2dbc.core.Bindings;
import net.ttddyy.dsproxy.r2dbc.core.ConnectionInfo;
import net.ttddyy.dsproxy.r2dbc.support.LastExecutionAwareListener;
import net.ttddyy.dsproxy.r2dbc.core.ProxyObject;
import net.ttddyy.dsproxy.r2dbc.core.QueryExecutionInfo;
import net.ttddyy.dsproxy.r2dbc.core.QueryInfo;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Tadaya Tsuyukubo
 */
public class ReactiveStatementCallbackTest {

    private static Method ADD_METHOD = ReflectionUtils.findMethod(Statement.class, "add");
    private static Method EXECUTE_METHOD = ReflectionUtils.findMethod(Statement.class, "execute");
    private static Method BIND_BY_INDEX_METHOD = ReflectionUtils.findMethod(Statement.class, "bind", int.class, Object.class);
    private static Method BIND_BY_ID_METHOD = ReflectionUtils.findMethod(Statement.class, "bind", Object.class, Object.class);
    private static Method BIND_NULL_BY_INDEX_METHOD = ReflectionUtils.findMethod(Statement.class, "bindNull", int.class, Class.class);
    private static Method BIND_NULL_BY_ID_METHOD = ReflectionUtils.findMethod(Statement.class, "bindNull", Object.class, Class.class);
    private static Method GET_TARGET_METHOD = ReflectionUtils.findMethod(ProxyObject.class, "getTarget");

    @Test
    void add() throws Throwable {
        LastExecutionAwareListener testListener = new LastExecutionAwareListener();

        ConnectionInfo connectionInfo = new ConnectionInfo();
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.addListener(testListener);
        Statement statement = mock(Statement.class);
        Statement mockResult = mock(Statement.class);

        when(statement.add()).thenReturn(mockResult);

        ReactiveStatementCallback callback = new ReactiveStatementCallback(statement, null, connectionInfo, proxyConfig);

        Object result = callback.invoke(null, ADD_METHOD, null);

        assertSame(mockResult, result);
    }

    @Test
    void executeOperationWithBindByIndex() throws Throwable {
        LastExecutionAwareListener testListener = new LastExecutionAwareListener();

        String query = "QUERY";
        ConnectionInfo connectionInfo = new ConnectionInfo();
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.addListener(testListener);
        Statement statement = mock(Statement.class);
        ReactiveStatementCallback callback = new ReactiveStatementCallback(statement, query, connectionInfo, proxyConfig);

        when(statement.execute()).thenReturn(Flux.empty());

        callback.invoke(null, BIND_BY_INDEX_METHOD, new Object[]{1, 100});
        callback.invoke(null, BIND_NULL_BY_INDEX_METHOD, new Object[]{2, String.class});
        callback.invoke(null, ADD_METHOD, null);
        callback.invoke(null, BIND_NULL_BY_INDEX_METHOD, new Object[]{1, int.class});
        callback.invoke(null, BIND_BY_INDEX_METHOD, new Object[]{2, 200});
        Object result = callback.invoke(null, EXECUTE_METHOD, null);


        StepVerifier.create((Publisher) result)
                .verifyComplete();

        QueryExecutionInfo afterQueryInfo = testListener.getAfterQueryExecutionInfo();

        assertNotNull(afterQueryInfo);

        assertEquals(0, afterQueryInfo.getBatchSize());
        assertEquals(2, afterQueryInfo.getBindingsSize());
        assertThat(afterQueryInfo.getQueries())
                .hasSize(1)
                .extracting(QueryInfo::getQuery)
                .containsExactly(query);
        QueryInfo queryInfo = afterQueryInfo.getQueries().get(0);

        assertThat(queryInfo.getBindingsList()).hasSize(2);
        Bindings firstBindings = queryInfo.getBindingsList().get(0);
        Bindings secondBindings = queryInfo.getBindingsList().get(1);


        assertThat(firstBindings.getIndexBindings())
                .hasSize(2)
                .extracting(Binding::getKey)
                .containsExactly(1, 2);
        assertThat(firstBindings.getIdentifierBindings()).isEmpty();

        List<BindingValue> bindingValues = firstBindings.getIndexBindings().stream()
                .map(Binding::getBindingValue)
                .collect(toList());

        // for "bind(1, 100)"
        assertThat(bindingValues.get(0))
                .isExactlyInstanceOf(BindingValue.SimpleBindingValue.class)
                .extracting(BindingValue::getValue)
                .isEqualTo(100);

        // for "bindNull(2, String.class)"
        assertThat(bindingValues.get(1))
                .isExactlyInstanceOf(BindingValue.NullBindingValue.class);
        BindingValue.NullBindingValue nullBindValue = (BindingValue.NullBindingValue) bindingValues.get(1);
        assertThat(nullBindValue.getType()).isEqualTo(String.class);


        assertThat(secondBindings.getIndexBindings())
                .hasSize(2)
                .extracting(Binding::getKey)
                .containsExactly(1, 2);
        assertThat(secondBindings.getIdentifierBindings()).isEmpty();

        bindingValues = secondBindings.getIndexBindings().stream()
                .map(Binding::getBindingValue)
                .collect(toList());

        // for "bindNull(1, int.class)"
        assertThat(bindingValues.get(0))
                .isExactlyInstanceOf(BindingValue.NullBindingValue.class);
        nullBindValue = (BindingValue.NullBindingValue) bindingValues.get(0);
        assertThat(nullBindValue.getType()).isEqualTo(int.class);

        // for "bind(2, 200)"
        assertThat(bindingValues.get(1))
                .isExactlyInstanceOf(BindingValue.SimpleBindingValue.class)
                .extracting(BindingValue::getValue)
                .isEqualTo(200);

    }

    @Test
    void executeOperationWithBindByIdentifier() throws Throwable {
        LastExecutionAwareListener testListener = new LastExecutionAwareListener();

        String query = "QUERY";
        ConnectionInfo connectionInfo = new ConnectionInfo();
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.addListener(testListener);
        Statement statement = mock(Statement.class);
        ReactiveStatementCallback callback = new ReactiveStatementCallback(statement, query, connectionInfo, proxyConfig);

        when(statement.execute()).thenReturn(Flux.empty());

        callback.invoke(null, BIND_BY_ID_METHOD, new Object[]{"$1", 100});
        callback.invoke(null, BIND_NULL_BY_ID_METHOD, new Object[]{"$2", String.class});
        callback.invoke(null, ADD_METHOD, null);
        callback.invoke(null, BIND_NULL_BY_ID_METHOD, new Object[]{"$1", int.class});
        callback.invoke(null, BIND_BY_ID_METHOD, new Object[]{"$2", 200});
        Object result = callback.invoke(null, EXECUTE_METHOD, null);


        StepVerifier.create((Publisher) result)
                .verifyComplete();

        QueryExecutionInfo afterQueryInfo = testListener.getAfterQueryExecutionInfo();

        assertNotNull(afterQueryInfo);

        assertEquals(0, afterQueryInfo.getBatchSize());
        assertEquals(2, afterQueryInfo.getBindingsSize());
        assertThat(afterQueryInfo.getQueries())
                .hasSize(1)
                .extracting(QueryInfo::getQuery)
                .containsExactly(query);
        QueryInfo queryInfo = afterQueryInfo.getQueries().get(0);

        assertThat(queryInfo.getBindingsList()).hasSize(2);
        Bindings firstBindings = queryInfo.getBindingsList().get(0);
        Bindings secondBindings = queryInfo.getBindingsList().get(1);


        assertThat(firstBindings.getIndexBindings()).isEmpty();
        assertThat(firstBindings.getIdentifierBindings())
                .hasSize(2)
                .extracting(Binding::getKey)
                .containsExactly("$1", "$2");

        List<BindingValue> bindingValues = firstBindings.getIdentifierBindings().stream()
                .map(Binding::getBindingValue)
                .collect(toList());

        // for "bind(1, 100)"
        assertThat(bindingValues.get(0))
                .isExactlyInstanceOf(BindingValue.SimpleBindingValue.class)
                .extracting(BindingValue::getValue)
                .isEqualTo(100);

        // for "bindNull(2, String.class)"
        assertThat(bindingValues.get(1))
                .isExactlyInstanceOf(BindingValue.NullBindingValue.class);
        BindingValue.NullBindingValue nullBindValue = (BindingValue.NullBindingValue) bindingValues.get(1);
        assertThat(nullBindValue.getType()).isEqualTo(String.class);


        assertThat(secondBindings.getIndexBindings()).isEmpty();
        assertThat(secondBindings.getIdentifierBindings())
                .hasSize(2)
                .extracting(Binding::getKey)
                .containsExactly("$1", "$2");

        bindingValues = secondBindings.getIdentifierBindings().stream()
                .map(Binding::getBindingValue)
                .collect(toList());

        // for "bindNull(1, int.class)"
        assertThat(bindingValues.get(0))
                .isExactlyInstanceOf(BindingValue.NullBindingValue.class);
        nullBindValue = (BindingValue.NullBindingValue) bindingValues.get(0);
        assertThat(nullBindValue.getType()).isEqualTo(int.class);

        // for "bind(2, 200)"
        assertThat(bindingValues.get(1))
                .isExactlyInstanceOf(BindingValue.SimpleBindingValue.class)
                .extracting(BindingValue::getValue)
                .isEqualTo(200);

    }

    @Test
    void getTarget() throws Throwable {
        Statement statement = mock(Statement.class);
        ConnectionInfo connectionInfo = new ConnectionInfo();
        ProxyConfig proxyConfig = new ProxyConfig();
        String query = "QUERY";

        ReactiveStatementCallback callback = new ReactiveStatementCallback(statement, query, connectionInfo, proxyConfig);

        Object result = callback.invoke(null, GET_TARGET_METHOD, null);
        assertSame(statement, result);
    }

}
