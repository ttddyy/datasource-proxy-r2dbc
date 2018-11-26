package net.ttddyy.dsproxy.r2dbc.support;

import net.ttddyy.dsproxy.r2dbc.core.MethodExecutionInfo;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

        AtomicReference<Method> invokedMethodHolder = new AtomicReference<>();

        // use spring aop framework to create a proxy of LifeCycleListener that just keeps the
        // invoked method
        MethodInterceptor interceptor = invocation -> {
            invokedMethodHolder.set(invocation.getMethod());
            return null;  // don't proceed the proxy
        };
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addAdvice(interceptor);
        proxyFactory.addInterface(LifeCycleListener.class);
        LifeCycleListener lifeCycleListener = (LifeCycleListener) proxyFactory.getProxy();

        LifeCycleExecutionListener listener = new LifeCycleExecutionListener(lifeCycleListener);

        MethodExecutionInfo methodExecutionInfo = mock(MethodExecutionInfo.class);

        Method[] declaredMethods = clazz.getDeclaredMethods();
        for (Method methodToInvoke : declaredMethods) {
            Method invokedMethod;
            String methodName = methodToInvoke.getName();

            // beforeXxxOnYyy : Xxx is a capitalized method-name and Yyy is a capitalized class-name
            String expectedBeforeMethodName = "before" + StringUtils.capitalize(methodName) + "On" + StringUtils.capitalize(className);
            String expectedAfterMethodName = "after" + StringUtils.capitalize(methodName) + "On" + StringUtils.capitalize(className);

            // mock executing method
            when(methodExecutionInfo.getMethod()).thenReturn(methodToInvoke);

            // invoke beforeMethod()
            listener.beforeMethod(methodExecutionInfo);

            // verify beforeMethod() invokes beforeXxxOnYyy() method
            invokedMethod = invokedMethodHolder.get();
            assertNotNull(invokedMethod);
            assertEquals(LifeCycleListener.class, invokedMethod.getDeclaringClass());
            assertEquals(expectedBeforeMethodName, invokedMethod.getName());


            // verify afterMethod() invokes afterXxxOnYyy() method
            listener.afterMethod(methodExecutionInfo);

            invokedMethod = invokedMethodHolder.get();
            assertNotNull(invokedMethod);
            assertEquals(LifeCycleListener.class, invokedMethod.getDeclaringClass());
            assertEquals(expectedAfterMethodName, invokedMethod.getName());

            // reset
            invokedMethodHolder.set(null);
            reset(methodExecutionInfo);
        }

    }
}
