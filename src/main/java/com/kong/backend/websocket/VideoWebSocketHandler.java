package com.kong.backend.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class VideoWebSocketHandler extends TextWebSocketHandler {

    private final List<WebSocketSession> userSessions = new CopyOnWriteArrayList<>();
    private final List<WebSocketSession> adminSessions = new CopyOnWriteArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String path = session.getUri().getPath();
        if (path.contains("/ws/admin/monitor")) {
            adminSessions.add(session);
            System.out.println("✅ 관리자 접속");
        } else if (path.contains("/ws/video")) {
            userSessions.add(session);
            System.out.println("✅ 사용자 접속");
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 관리자에게만 브로드캐스트
        for (WebSocketSession admin : adminSessions) {
            if (admin.isOpen()) {
                admin.sendMessage(message);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        userSessions.remove(session);
        adminSessions.remove(session);
    }
}
