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
    private final ObjectMapper mapper = new ObjectMapper();

    private final List<WebSocketSession> videoSessions = new CopyOnWriteArrayList<>();
    private final List<WebSocketSession> alertSessions = new CopyOnWriteArrayList<>();
    private WebSocketSession yoloSession;

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
                                        int userKey = 1; // TODO: 실제 사용자 키로 교체
                                        alertService.saveAlert(alertLevel, eventType, detectedAt, userKey);
                                        System.out.println("✅ 알림 저장 완료: " + alertLevel + ", " + eventType + " at " + detectedAtStr);
                                    } catch (Exception e) {
                                        System.out.println("❌ 날짜 파싱 오류: " + detectedAtStr + " - " + e.getMessage());
                                    }

                                    broadcastTo(alertSessions, message);
                                } catch (Exception e) {
                                    System.out.println("❌ YOLO 메시지 파싱 실패: " + e.getMessage());
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

                        }, "ws://15.165.114.170:8765/ws/fall");

                        // break 제거 → 항상 연결 시도 유지
                    }

                    Thread.sleep(3000); // 매 3초마다 연결 확인
                } catch (Exception e) {
                    System.out.println("🚨 YOLO 연결 실패: " + e.getMessage());
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

        if (path.contains("/ws/video")) {
            videoSessions.add(session);
            System.out.println("✅ 사용자 앱(WebSocket 영상) 연결됨");
        } else if (path.contains("/ws/admin/monitor")) {
            videoSessions.add(session); // 관리자도 프레임 수신 대상
            System.out.println("✅ 관리자 모니터링(WebSocket 영상) 연결됨");
        } else if (path.contains("/ws/alert")) {
            alertSessions.add(session);
            System.out.println("✅ 관리자 알림 수신(WebSocket 알림) 연결됨");
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            // 사용자 앱이 보낸 프레임 → YOLO 서버로 전달
            if (yoloSession != null && yoloSession.isOpen()) {
                synchronized (yoloSession) {
                    yoloSession.sendMessage(message);
                }
            }

            // 관리자/사용자 프레임 수신 대상에게 중계
            broadcastTo(videoSessions, message);

        } catch (Exception e) {
            System.out.println("❌ YOLO 서버 전송 실패: " + e.getMessage());
        }
    }

    private void broadcastTo(List<WebSocketSession> sessions, TextMessage message) {
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(message);
                    }
                }
            } catch (Exception e) {
                System.out.println("❌ 전송 실패: " + e.getMessage());
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        videoSessions.remove(session);
        alertSessions.remove(session);
        if (session == yoloSession) {
            yoloSession = null;
        }
    }
}
