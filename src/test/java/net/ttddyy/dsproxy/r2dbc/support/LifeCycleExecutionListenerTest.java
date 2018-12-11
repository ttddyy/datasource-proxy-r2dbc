package net.ttddyy.dsproxy.r2dbc.support;

import net.ttddyy.dsproxy.r2dbc.core.ExecutionType;
import net.ttddyy.dsproxy.r2dbc.core.MethodExecutionInfo;
import net.ttddyy.dsproxy.r2dbc.core.QueryExecutionInfo;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * @author Tadaya Tsuyukubo
 */
public class LifeCycleExecutionListenerTest {

    /**
     * Test invoking {@link LifeCycleExecutionListener#beforeMethod(MethodExecutionInfo)} and
     * {@link LifeCycleExecutionListener#afterMethod(MethodExecutionInfo)} invokes corresponding
     * before/after methods defined on {@link LifeCycleListener}.
     *
     * @param clazz class that datasource-proxy-r2dbc creates proxy
     */
    @ParameterizedTest
    @ProxyClassesSource
    void methodInvocations(Class<?> clazz) {
        String className = clazz.getSimpleName();

        List<Method> invokedMethods = new ArrayList<>();
        LifeCycleListener lifeCycleListener = createLifeCycleListener(invokedMethods);
        LifeCycleExecutionListener listener = new LifeCycleExecutionListener(lifeCycleListener);

        MethodExecutionInfo methodExecutionInfo = mock(MethodExecutionInfo.class);

        Method[] declaredMethods = clazz.getDeclaredMethods();
        for (Method methodToInvoke : declaredMethods) {
            String methodName = methodToInvoke.getName();

            // beforeXxxOnYyy : Xxx is a capitalized method-name and Yyy is a capitalized class-name
            String expectedBeforeMethodName = "before" + StringUtils.capitalize(methodName) + "On" + StringUtils.capitalize(className);
            String expectedAfterMethodName = "after" + StringUtils.capitalize(methodName) + "On" + StringUtils.capitalize(className);

            // mock executing method
            when(methodExecutionInfo.getMethod()).thenReturn(methodToInvoke);

            // invoke beforeMethod()
            listener.beforeMethod(methodExecutionInfo);

            // first method is beforeMethod
            // second method is beforeXxxOnYyy
            assertThat(invokedMethods)
                    .hasSize(2)
                    .extracting(Method::getName)
                    .containsExactly("beforeMethod", expectedBeforeMethodName)
            ;

            // extra check for beforeXxxOnYyy
            Method beforeXxxOnYyy = invokedMethods.get(1);
            assertEquals(LifeCycleListener.class, beforeXxxOnYyy.getDeclaringClass());

            // reset
            invokedMethods.clear();

            listener.afterMethod(methodExecutionInfo);

            // first method is afterXxxOnYyy
            // second method is afterMethod
            assertThat(invokedMethods)
                    .hasSize(2)
                    .extracting(Method::getName)
                    .containsExactly(expectedAfterMethodName, "afterMethod")
            ;

            // extra check for afterXxxOnYyy
            Method afterXxxOnYyy = invokedMethods.get(0);
            assertEquals(LifeCycleListener.class, afterXxxOnYyy.getDeclaringClass());

            // reset
            invokedMethods.clear();
            reset(methodExecutionInfo);
        }

    }

    @Test
    void queryExecution() {

        List<Method> invokedMethods = new ArrayList<>();
        LifeCycleListener lifeCycleListener = createLifeCycleListener(invokedMethods);
        LifeCycleExecutionListener listener = new LifeCycleExecutionListener(lifeCycleListener);

        QueryExecutionInfo queryExecutionInfo;

        // for Statement#execute
        queryExecutionInfo = new QueryExecutionInfo();
        queryExecutionInfo.setType(ExecutionType.STATEMENT);

        // test beforeQuery
        listener.beforeQuery(queryExecutionInfo);
        verifyQueryExecutionInvocation(invokedMethods, "beforeQuery", "beforeExecuteOnStatement");

        invokedMethods.clear();

        // test afterQuery
        listener.afterQuery(queryExecutionInfo);
        verifyQueryExecutionInvocation(invokedMethods, "afterExecuteOnStatement", "afterQuery");

        assertEquals(LifeCycleListener.class, invokedMethods.get(0).getDeclaringClass());
        assertEquals(LifeCycleListener.class, invokedMethods.get(1).getDeclaringClass());

        invokedMethods.clear();


        // for Batch#execute
        queryExecutionInfo = new QueryExecutionInfo();
        queryExecutionInfo.setType(ExecutionType.BATCH);

        // test beforeQuery
        listener.beforeQuery(queryExecutionInfo);
        verifyQueryExecutionInvocation(invokedMethods, "beforeQuery", "beforeExecuteOnBatch");
        invokedMethods.clear();

        // test afterQuery
        listener.afterQuery(queryExecutionInfo);
        verifyQueryExecutionInvocation(invokedMethods, "afterExecuteOnBatch", "afterQuery");

    }

    private void verifyQueryExecutionInvocation(List<Method> invokedMethods, String... expectedMethodNames) {
        assertThat(invokedMethods)
                .hasSize(2)
                .extracting(Method::getName)
                .containsExactly(expectedMethodNames)
        ;

        assertEquals(LifeCycleListener.class, invokedMethods.get(0).getDeclaringClass());
        assertEquals(LifeCycleListener.class, invokedMethods.get(1).getDeclaringClass());

    }

    private LifeCycleListener createLifeCycleListener(List<Method> invokedMethods) {
        // use spring aop framework to create a proxy of LifeCycleListener that just keeps the
        // invoked methods
        MethodInterceptor interceptor = invocation -> {
            invokedMethods.add(invocation.getMethod());
            return null;  // don't proceed the proxy
        };

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvice(interceptor);
        proxyFactory.addInterface(LifeCycleListener.class);
        return (LifeCycleListener) proxyFactory.getProxy();
    }


    // TODO: add test for onEachQueryResult


}
