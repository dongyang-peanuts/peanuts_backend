package com.kong.backend.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kong.backend.service.AlertService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@RequiredArgsConstructor
public class VideoWebSocketHandler extends TextWebSocketHandler {

    private final AlertService alertService;
    private final List<WebSocketSession> userSessions = new CopyOnWriteArrayList<>();
    private final List<WebSocketSession> adminSessions = new CopyOnWriteArrayList<>();
    private WebSocketSession yoloSession;
    private final ObjectMapper mapper = new ObjectMapper();

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

                @Override
                protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                    String payload = message.getPayload();
                    System.out.println("📥 YOLO 서버 결과 수신: " + payload);

                    try {
                        JsonNode json = mapper.readTree(payload);
                        int userKey = 1; // 고정값

                        String alertLevel = json.get("alertLevel").asText();
                        String eventType = json.get("eventType").asText();
                        LocalDateTime detectedAt = LocalDateTime.parse(json.get("detectedAt").asText());

                        alertService.saveAlert(alertLevel, eventType, detectedAt, userKey);
                    } catch (Exception e) {
                        System.out.println("❌ 파싱 또는 저장 실패: " + e.getMessage());
                    }

                    // 관리자에게 전달
                    broadcastMessageTo(adminSessions, message);

                    // 사용자에게 전달
                    broadcastMessageTo(userSessions, message);
                }
            }, "ws://15.165.114.170:8765/ws/fall");

        } catch (Exception e) {
            System.out.println("❌ YOLO 서버 연결 실패: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
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
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        broadcastMessageTo(adminSessions, message);
        broadcastMessageTo(userSessions, message, session);

        if (yoloSession != null && yoloSession.isOpen()) {
            try {
                synchronized (yoloSession) {
                    yoloSession.sendMessage(message);
                }
            } catch (Exception e) {
                System.out.println("❌ YOLO 서버로 전송 실패: " + e.getMessage());
            }
        }
    }

    private void broadcastMessageTo(List<WebSocketSession> sessions, TextMessage message) {
        broadcastMessageTo(sessions, message, null);
    }

    private void broadcastMessageTo(List<WebSocketSession> sessions, TextMessage message, WebSocketSession exclude) {
        for (WebSocketSession s : sessions) {
            if (s.isOpen() && (exclude == null || !s.getId().equals(exclude.getId()))) {
                try {
                    synchronized (s) {
                        s.sendMessage(message);
                    }
                } catch (Exception e) {
                    System.out.println("❌ 메시지 전송 실패: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        userSessions.remove(session);
        adminSessions.remove(session);
        if (yoloSession == session) {
            yoloSession = null;
        }
    }
}
