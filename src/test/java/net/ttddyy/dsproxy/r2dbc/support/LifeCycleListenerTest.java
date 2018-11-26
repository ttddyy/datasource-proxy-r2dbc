package net.ttddyy.dsproxy.r2dbc.support;

import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * @author Tadaya Tsuyukubo
 */
public class LifeCycleListenerTest {

    @ParameterizedTest
    @ProxyClassesSource
    void verifyMethodNames(Class<?> clazz) {

        String className = clazz.getSimpleName();

        Set<String> expected = Stream.of(clazz.getDeclaredMethods())
                .map(Method::getName)
                .flatMap(methodName -> {
                    // beforeXxxOnYyy / afterXxxOnYyy
                    String name = StringUtils.capitalize(methodName) + "On" + StringUtils.capitalize(className);
                    return Stream.of("before" + name, "after" + name);
                })
                .collect(toSet());

        Set<String> actual = Stream.of(LifeCycleListener.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(toSet());

        assertThat(actual).containsAll(expected);
    }

}
