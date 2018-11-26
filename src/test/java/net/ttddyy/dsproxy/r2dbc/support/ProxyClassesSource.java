package net.ttddyy.dsproxy.r2dbc.support;

import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Provides classes that datasource-proxy-r2dbc generates proxies.
 *
 * Composed annotation for {@link org.junit.jupiter.params.ParameterizedTest}.
 *
 * @author Tadaya Tsuyukubo
 */
@Retention(RetentionPolicy.RUNTIME)
@ValueSource(classes = {ConnectionFactory.class, Connection.class, Batch.class, Statement.class, Result.class})
public @interface ProxyClassesSource {
}
