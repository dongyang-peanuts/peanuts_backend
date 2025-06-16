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
import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
public class VideoWebSocketHandler extends TextWebSocketHandler {

    private final AlertService alertService;
    private final ObjectMapper mapper = new ObjectMapper();

    private final List<WebSocketSession> videoSessions = new CopyOnWriteArrayList<>();
    private final List<WebSocketSession> alertSessions = new CopyOnWriteArrayList<>();
    private volatile WebSocketSession yoloSession; // ⚠ volatile로 동기화

    private final BlockingQueue<String> yoloQueue = new LinkedBlockingQueue<>();

    @PostConstruct
    public void init() {
        startYoloConnectionLoop();
        startYoloSendingThread();
    }

    private void startYoloConnectionLoop() {
        Executors.newSingleThreadExecutor().submit(() -> {
            while (true) {
                try {
                    if (yoloSession == null || !yoloSession.isOpen()) {
                        System.out.println("🔄 YOLO 서버 연결 시도 중...");

                        WebSocketClient client = new StandardWebSocketClient();
                        client.doHandshake(new TextWebSocketHandler() {
                            @Override
                            public void afterConnectionEstablished(WebSocketSession session) {
                                if (yoloSession != null && yoloSession.isOpen()) {
                                    try {
                                        yoloSession.close(); // 이전 연결 종료
                                        System.out.println("🔁 이전 YOLO 세션 종료");
                                    } catch (Exception e) {
                                        System.out.println("❌ 이전 YOLO 세션 종료 실패: " + e.getMessage());
                                    }
                                }
                                yoloSession = session;
                                System.out.println("✅ YOLO 서버와 연결됨");
                            }

                            @Override
                            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                                String payload = message.getPayload();
                                System.out.println("📥 YOLO 결과 수신: " + payload);

                                try {
                                    JsonNode json = mapper.readTree(payload);
                                    String alertLevel = json.get("alertLevel").asText();
                                    String eventType = json.get("eventType").asText();
                                    String detectedAtStr = json.get("detectedAt").asText();

                                    try {
                                        LocalDateTime detectedAt = LocalDateTime.parse(detectedAtStr);
                                        int userKey = 1;
                                        alertService.saveAlert(alertLevel, eventType, detectedAt, userKey);
                                        System.out.println("✅ 알림 저장 완료");
                                    } catch (Exception e) {
                                        System.out.println("❌ 날짜 파싱 오류: " + detectedAtStr + " - " + e.getMessage());
                                    }

                                    broadcastTo(alertSessions, new TextMessage(payload));
                                } catch (Exception e) {
                                    System.out.println("❌ YOLO 메시지 파싱 실패: " + e.getMessage());
                                }
                            }

                            @Override
                            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                                System.out.println("❌ YOLO 연결 종료됨: " + status);
                                if (session == yoloSession) {
                                    yoloSession = null;
                                }
                            }

                            @Override
                            public void handleTransportError(WebSocketSession session, Throwable exception) {
                                System.out.println("❌ YOLO 연결 오류: " + exception.getMessage());
                                if (session == yoloSession) {
                                    try {
                                        session.close();
                                    } catch (Exception ignored) {}
                                    yoloSession = null;
                                }
                            }

                        }, "ws://15.165.114.170:8765/ws/fall");
                    }

                    Thread.sleep(3000);
                } catch (Exception e) {
                    System.out.println("🚨 YOLO 연결 실패: " + e.getMessage());
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ignored) {}
                }
            }
        });
    }

    private void startYoloSendingThread() {
        Executors.newSingleThreadExecutor().submit(() -> {
            while (true) {
                try {
                    String message = yoloQueue.take();
                    if (yoloSession != null && yoloSession.isOpen()) {
                        synchronized (yoloSession) {
                            yoloSession.sendMessage(new TextMessage(message));
                        }
                    } else {
                        System.out.println("⚠️ YOLO 세션 없음. 전송 보류됨.");
                    }
                } catch (Exception e) {
                    System.out.println("❌ YOLO 전송 오류: " + e.getMessage());
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
            videoSessions.add(session);
            System.out.println("✅ 관리자(WebSocket 영상) 연결됨");
        } else if (path.contains("/ws/alert")) {
            alertSessions.add(session);
            System.out.println("✅ 관리자(WebSocket 알림) 연결됨");
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            broadcastTo(videoSessions, message);
            yoloQueue.offer(message.getPayload());
        } catch (Exception e) {
            System.out.println("❌ 프레임 처리 오류: " + e.getMessage());
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
                System.out.println("❌ 클라이언트 전송 실패: " + e.getMessage());
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        videoSessions.remove(session);
        alertSessions.remove(session);
        if (session == yoloSession) {
            yoloSession = null;
            System.out.println("🧹 YOLO 세션 정리 완료");
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        System.out.println("❌ WebSocket 전송 오류: " + exception.getMessage());
        videoSessions.remove(session);
        alertSessions.remove(session);
        if (session == yoloSession) {
            try {
                session.close();
            } catch (Exception ignored) {}
            yoloSession = null;
        }
    }
}
