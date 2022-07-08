package com.example.websocket;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.websocket.WebSocketClient;
import io.micronaut.websocket.annotation.ClientWebSocket;
import io.micronaut.websocket.annotation.OnMessage;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import javax.validation.constraints.NotBlank;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Property(name = "spec.name", value = "NotifyWebSocketTest")
@MicronautTest
public class NotifyServerWebSocketTest {

    @Inject
    BeanContext beanContext;

    @Inject
    EmbeddedServer embeddedServer;

    @Requires(property = "spec.name", value = "NotifyWebSocketTest")
    @ClientWebSocket
    static abstract class TestWebSocketClient implements AutoCloseable {

        private final Deque<String> messageHistory = new ConcurrentLinkedDeque<>();

        public String getLatestMessage() {
            return messageHistory.peekLast();
        }

        public List<String> getMessagesChronologically() {
            return new ArrayList<>(messageHistory);
        }

        @OnMessage
        void onMessage(String message) {
            messageHistory.add(message);
        }

        abstract void send(@NonNull @NotBlank String message);
    }

    private TestWebSocketClient createWebSocketClient(int port, String to) {
        WebSocketClient webSocketClient = beanContext.getBean(WebSocketClient.class);
        URI uri = UriBuilder.of("ws://localhost")
                .port(port)
                .path("ws")
                .path("notify")
                .path("{to}")
                .expand(CollectionUtils.mapOf( "to", to));
        Publisher<TestWebSocketClient> client = webSocketClient.connect(TestWebSocketClient.class,  uri);
        return Flux.from(client).blockFirst();
    }

    @Test
    void testWebsockerServer() throws Exception {
        TestWebSocketClient adamNotifications = createWebSocketClient(embeddedServer.getPort(), "adam");
        await().until(() ->
                Collections.singletonList("Listen [adam] Notifications!")
                        .equals(adamNotifications.getMessagesChronologically()));

        TestWebSocketClient annaNotifications = createWebSocketClient(embeddedServer.getPort(), "anna");
        await().until(() ->
                Collections.singletonList("Listen [anna] Notifications!")
                        .equals(annaNotifications.getMessagesChronologically()));

        TestWebSocketClient benNotifications = createWebSocketClient(embeddedServer.getPort(), "ben");
        await().until(() ->
                Collections.singletonList("Listen [ben] Notifications!")
                        .equals(benNotifications.getMessagesChronologically()));

        final String notifyToAdam = "Adam, here is your notification!)";
        adamNotifications.send(notifyToAdam);

        await().until(() ->
                notifyToAdam.equals(adamNotifications.getLatestMessage()));

        final String notifyToAnna = "Anna, here is your notification!)";
        annaNotifications.send(notifyToAnna);

        await().until(() ->
                notifyToAnna.equals(annaNotifications.getLatestMessage()));

        final String notifyToBen = "Ben, here is your notification!)";
        benNotifications.send(notifyToBen);

        //Others users cannnot listen anothers notifications
        assertNotEquals(annaNotifications.getLatestMessage(), benNotifications.getLatestMessage());

        annaNotifications.close();
        adamNotifications.close();
        benNotifications.close();
    }
}
