package net.ttddyy.dsproxy.r2dbc.support;

import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
    @ValueSource(classes = {ConnectionFactory.class, Connection.class, Batch.class, Statement.class, Result.class})
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
