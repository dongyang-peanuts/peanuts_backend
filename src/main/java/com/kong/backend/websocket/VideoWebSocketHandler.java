package com.kong.backend.websocket;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class VideoWebSocketHandler extends TextWebSocketHandler {

    private final List<WebSocketSession> userSessions = new CopyOnWriteArrayList<>();
    private final List<WebSocketSession> adminSessions = new CopyOnWriteArrayList<>();

    private WebSocketSession yoloSession;

    @PostConstruct
    public void connectToYoloServer() {
        try {
            WebSocketClient client = new StandardWebSocketClient();
            client.doHandshake(new TextWebSocketHandler() {
                @Override
                public void afterConnectionEstablished(WebSocketSession session) {
                    yoloSession = session;
                    System.out.println("✅ YOLO 서버와 연결됨");
                }
            }, "ws://192.168.219.171:8000/ws/fall");  // 실제 YOLO Python 서버 주소
        } catch (Exception e) {
            System.out.println("❌ YOLO 서버 연결 실패: " + e.getMessage());
        }
    }

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
        // 관리자에게 브로드캐스트
        for (WebSocketSession admin : adminSessions) {
            if (admin.isOpen()) {
                admin.sendMessage(message);
            }
        }

        // 사용자에게 브로드캐스트 (본인 제외)
        for (WebSocketSession user : userSessions) {
            if (user.isOpen() && !user.getId().equals(session.getId())) {
                user.sendMessage(message);
            }
        }

        // ✅ YOLO 서버에도 중계 전송
        if (yoloSession != null && yoloSession.isOpen()) {
            yoloSession.sendMessage(message);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        userSessions.remove(session);
        adminSessions.remove(session);
    }
}
