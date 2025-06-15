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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

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
        Executors.newSingleThreadExecutor().submit(() -> {
            while (true) {
                try {
                    WebSocketClient client = new StandardWebSocketClient();
                    client.doHandshake(new TextWebSocketHandler() {
                        @Override
                        public void afterConnectionEstablished(WebSocketSession session) {
                            yoloSession = session;
                            System.out.println("✅ YOLO 서버와 연결됨");
                        }

                        @Override
                        protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                            String payload = message.getPayload();
                            System.out.println("📥 YOLO 서버 결과 수신: " + payload);
                            try {
                                JsonNode json = mapper.readTree(payload);
                                int userKey = 1; // 예시: 고정 사용자
                                String alertLevel = json.get("alertLevel").asText();
                                String eventType = json.get("eventType").asText();
                                String detectedAt = json.get("detectedAt").asText();
                                alertService.saveAlert(alertLevel, eventType, detectedAt, userKey);
                            } catch (Exception e) {
                                System.out.println("❌ 알림 처리 실패: " + e.getMessage());
                            }

                            // 브로드캐스트
                            broadcastMessageTo(adminSessions, message);
                            broadcastMessageTo(userSessions, message);
                        }

                        @Override
                        public void handleTransportError(WebSocketSession session, Throwable exception) {
                            System.out.println("❌ YOLO 서버 오류: " + exception.getMessage());
                            yoloSession = null;
                        }

                        @Override
                        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                            System.out.println("❌ YOLO 서버 연결 종료");
                            yoloSession = null;
                        }

                    }, "ws://15.165.114.170:8765/ws/fall");

                    break;
                } catch (Exception e) {
                    System.out.println("🚨 YOLO 연결 실패, 3초 후 재시도...");
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ignored) {}
                }
            }
        });
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
        // 사용자에게서 들어온 base64 JSON -> YOLO 서버로 전달
        if (yoloSession != null && yoloSession.isOpen()) {
            try {
                synchronized (yoloSession) {
                    yoloSession.sendMessage(message);
                }
            } catch (Exception e) {
                System.out.println("❌ YOLO 전송 실패: " + e.getMessage());
            }
        }
    }

    private void broadcastMessageTo(List<WebSocketSession> sessions, TextMessage message) {
        for (WebSocketSession s : sessions) {
            if (s.isOpen()) {
                try {
                    synchronized (s) {
                        s.sendMessage(message);
                    }
                } catch (Exception e) {
                    System.out.println("❌ 메시지 전송 오류: " + e.getMessage());
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
