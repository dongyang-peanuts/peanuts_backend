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
                    if (yoloSession == null || !yoloSession.isOpen()) {
                        System.out.println("🔄 YOLO 서버 연결 시도 중...");

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
                                    String alertLevel = json.get("alertLevel").asText();
                                    String eventType = json.get("eventType").asText();
                                    String detectedAtStr = json.get("detectedAt").asText();

                                    try {
                                        LocalDateTime detectedAt = LocalDateTime.parse(detectedAtStr);
                                        int userKey = 1; // 실제 사용자 키로 교체 가능
                                        alertService.saveAlert(alertLevel, eventType, detectedAt, userKey);
                                        System.out.println("✅ 알림 저장 완료: " + alertLevel + ", " + eventType + " at " + detectedAtStr);
                                    } catch (Exception e) {
                                        System.out.println("❌ 날짜 파싱 오류: " + detectedAtStr + " - " + e.getMessage());
                                    }

                                    broadcastToAll(message);
                                } catch (Exception e) {
                                    System.out.println("❌ 파싱 또는 저장 실패: " + e.getMessage());
                                }
                            }

                            @Override
                            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                                System.out.println("❌ YOLO 서버 연결 종료됨: " + status);
                                yoloSession = null;
                            }

                            @Override
                            public void handleTransportError(WebSocketSession session, Throwable exception) {
                                System.out.println("❌ YOLO 서버 연결 오류: " + exception.getMessage());
                                yoloSession = null;
                            }

                        }, "ws://15.165.114.170:8765/ws/fall"); // 실제 YOLO 서버 주소 사용

                        // break 제거 → 항상 연결 유지
                    }
                    Thread.sleep(3000); // 매 3초마다 연결 상태 확인
                } catch (Exception e) {
                    System.out.println("🚨 YOLO 연결 실패 또는 예외 발생: " + e.getMessage());
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
        try {
            if (yoloSession != null && yoloSession.isOpen()) {
                synchronized (yoloSession) {
                    yoloSession.sendMessage(message);
                }
            }
        } catch (Exception e) {
            System.out.println("❌ YOLO 서버로 전송 실패: " + e.getMessage());
        }
    }

    private void broadcastToAll(TextMessage message) {
        for (WebSocketSession session : userSessions) {
            try {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(message);
                    }
                }
            } catch (Exception e) {
                System.out.println("❌ 사용자 전송 실패: " + e.getMessage());
            }
        }

        for (WebSocketSession session : adminSessions) {
            try {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(message);
                    }
                }
            } catch (Exception e) {
                System.out.println("❌ 관리자 전송 실패: " + e.getMessage());
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        userSessions.remove(session);
        adminSessions.remove(session);
        if (session == yoloSession) {
            yoloSession = null;
        }
    }
}
