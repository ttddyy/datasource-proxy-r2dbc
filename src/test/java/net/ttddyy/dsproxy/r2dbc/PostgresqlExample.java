/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ttddyy.dsproxy.r2dbc;

import com.zaxxer.hikari.HikariDataSource;
import io.r2dbc.client.R2dbc;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import net.ttddyy.dsproxy.r2dbc.support.MethodExecutionInfoFormatter;
import net.ttddyy.dsproxy.r2dbc.support.QueryExecutionInfoFormatter;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.annotation.Nullable;

import java.io.IOException;

/**
 * Copy from r2dbc-client
 */
final class PostgresqlExample implements Example<String> {

    @RegisterExtension
    static final PostgresqlServerExtension SERVER = new PostgresqlServerExtension();

    private final PostgresqlConnectionConfiguration configuration = PostgresqlConnectionConfiguration.builder()
        .database(SERVER.getDatabase())
        .host(SERVER.getHost())
        .port(SERVER.getPort())
        .password(SERVER.getPassword())
        .username(SERVER.getUsername())
        .build();

//        private final R2dbc r2dbc = new R2dbc(new PostgresqlConnectionFactory(this.configuration));
    private R2dbc r2dbc ;
    {
        ConnectionFactory connectionFactory = new PostgresqlConnectionFactory(this.configuration);

        Logger logger = Loggers.getLogger(getClass());

        QueryExecutionInfoFormatter queryExecutionFormatter = QueryExecutionInfoFormatter.showAll();
        MethodExecutionInfoFormatter methodExecutionFormatter = MethodExecutionInfoFormatter.withDefault();

        ConnectionFactory proxyConnectionFactory =
                ProxyConnectionFactoryBuilder.create(connectionFactory)
                        .onAfterMethod(execInfo ->
                                execInfo.map(methodExecutionFormatter::format)
                                        .doOnNext(System.out::println)
                                        .subscribe())
                        .onEachQueryResult(result -> {
                            result.doOnNext(executionInfo -> {
                                int currentResultCount = executionInfo.getCurrentResultCount();
                                Object currentResult = executionInfo.getCurrentMappedResult();

                                System.out.println("RESULT_NO=" + currentResultCount + " RESULT=" + currentResult);
                            }).subscribe();
                        })
                        .onAfterQuery(execInfo ->
                                execInfo.map(queryExecutionFormatter::format)
                                        .doOnNext(System.out::println)
                                        .subscribe())
                        .build();

        this.r2dbc = new R2dbc(proxyConnectionFactory);
    }

    @Override
    public String getIdentifier(int index) {
        return getPlaceholder(index);
    }

    @Override
    public JdbcOperations getJdbcOperations() {
        JdbcOperations jdbcOperations = SERVER.getJdbcOperations();

        if (jdbcOperations == null) {
            throw new IllegalStateException("JdbcOperations not yet initialized");
        }

        return jdbcOperations;
    }

    @Override
    public String getPlaceholder(int index) {
        return String.format("$%d", index + 1);
    }

    @Override
    public R2dbc getR2dbc() {
        return this.r2dbc;
    }

    private static final class PostgresqlServerExtension implements BeforeAllCallback, AfterAllCallback {

        private final PostgreSQLContainer<?> container = new PostgreSQLContainer<>();

        private HikariDataSource dataSource;

        private JdbcOperations jdbcOperations;

        @Override
        public void afterAll(ExtensionContext context) {
            this.dataSource.close();
            this.container.stop();
        }

        @Override
        public void beforeAll(ExtensionContext context) throws IOException {
            this.container.start();

            this.dataSource = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(this.container.getJdbcUrl())
                .username(this.container.getUsername())
                .password(this.container.getPassword())
                .build();

            this.dataSource.setMaximumPoolSize(1);

            this.jdbcOperations = new JdbcTemplate(this.dataSource);
        }

        String getDatabase() {
            return this.container.getDatabaseName();
        }

        String getHost() {
            return this.container.getContainerIpAddress();
        }

        @Nullable
        JdbcOperations getJdbcOperations() {
            return this.jdbcOperations;
        }

        String getPassword() {
            return this.container.getPassword();
        }

        int getPort() {
            return this.container.getMappedPort(5432);
        }

        String getUsername() {
            return this.container.getUsername();
        }

    }
}
