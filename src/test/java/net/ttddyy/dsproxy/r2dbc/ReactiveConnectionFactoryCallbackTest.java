package net.ttddyy.dsproxy.r2dbc;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import net.ttddyy.dsproxy.r2dbc.core.ConnectionIdManager;
import net.ttddyy.dsproxy.r2dbc.core.ConnectionInfo;
import net.ttddyy.dsproxy.r2dbc.core.LastExecutionAwareListener;
import net.ttddyy.dsproxy.r2dbc.core.MethodExecutionInfo;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Tadaya Tsuyukubo
 */
public class ReactiveConnectionFactoryCallbackTest {

    private static Method CREATE_METHOD = ReflectionUtils.findMethod(ConnectionFactory.class, "create");
    private static Method GET_METADATA_METHOD = ReflectionUtils.findMethod(ConnectionFactory.class, "getMetadata");

    @Test
    void createConnection() throws Throwable {

        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        Connection originalConnection = mock(Connection.class);
        Connection mockedConnection = mock(Connection.class);
        ConnectionIdManager idManager = mock(ConnectionIdManager.class);
        ProxyFactory proxyFactory = mock(ProxyFactory.class);

        LastExecutionAwareListener listener = new LastExecutionAwareListener();

        // mock call to original ConnectionFactory#create()
        doReturn(Mono.just(originalConnection)).when(connectionFactory).create();

        String connectionId = "100";
        when(idManager.getId(originalConnection)).thenReturn(connectionId);

        // mock where it creates proxied connection
        when(proxyFactory.createConnection(any(Connection.class), any(ConnectionInfo.class))).thenReturn(mockedConnection);


        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setConnectionIdManager(idManager);
        proxyConfig.setProxyFactory(proxyFactory);
        proxyConfig.addListener(listener);

        ReactiveConnectionFactoryCallback callback = new ReactiveConnectionFactoryCallback(connectionFactory, proxyConfig);

        Object result = callback.invoke(null, CREATE_METHOD, null);

        assertThat(result).isInstanceOf(Publisher.class);

        StepVerifier.create((Publisher<?>) result)
                .expectSubscription()
                .assertNext(object -> {
                    assertSame(mockedConnection, object);
                })
                .verifyComplete();

        MethodExecutionInfo afterMethod = listener.getAfterMethodExecutionInfo();
        assertNotNull(afterMethod);
        ConnectionInfo connectionInfo = afterMethod.getConnectionInfo();
        assertNotNull(connectionInfo);
        assertEquals(connectionId, connectionInfo.getConnectionId());
        assertFalse(connectionInfo.isClosed());

        assertSame(connectionFactory, afterMethod.getTarget());
        assertSame(originalConnection, afterMethod.getResult());
    }

    @Test
    void getMetadata() throws Throwable {

        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        ConnectionFactoryMetadata metadata = mock(ConnectionFactoryMetadata.class);

        when(connectionFactory.getMetadata()).thenReturn(metadata);

        ProxyConfig proxyConfig = new ProxyConfig();
        ReactiveConnectionFactoryCallback callback = new ReactiveConnectionFactoryCallback(connectionFactory, proxyConfig);

        Object result = callback.invoke(null, GET_METADATA_METHOD, null);

        assertSame(metadata, result);

    }

}
