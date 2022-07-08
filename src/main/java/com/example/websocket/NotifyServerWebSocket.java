package com.example.websocket;

import io.micronaut.websocket.WebSocketBroadcaster;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.micronaut.websocket.annotation.ServerWebSocket;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;

@ServerWebSocket("/ws/notify/{to}")
public class NotifyServerWebSocket {

    private static final Logger LOG = LoggerFactory.getLogger(NotifyServerWebSocket.class);

    private final WebSocketBroadcaster broadcaster;

    public NotifyServerWebSocket(WebSocketBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @OnOpen
    public Publisher<String> onOpen(String to, WebSocketSession session) {
        log("onOpen", session, to);
        return broadcaster.broadcast(String.format("Listen [%s] Notifications!", to), isValid(to));
    }

    @OnMessage
    public Publisher<String> onMessage(
            String to,
            String notification,
            WebSocketSession session) {

        log("onMessage", session, to);
        return broadcaster.broadcast(notification, isValid(to));
    }

    @OnClose
    public Publisher<String> onClose(
            String to,
            WebSocketSession session) {

        log("onClose", session, to);
        return broadcaster.broadcast(String.format("Stop listen [%s] notifications!", to), isValid(to));
    }

    private void log(String event, WebSocketSession session, String to) {
        LOG.info("* WebSocket: {} received for session {} to '{}'",
                event, session.getId(), to);
    }

    private Predicate<WebSocketSession> isValid(String to) {
        return s -> to.equals("all") //broadcast to all users
                || to.equalsIgnoreCase(s.getUriVariables().get("to", String.class, null));
    }
}
