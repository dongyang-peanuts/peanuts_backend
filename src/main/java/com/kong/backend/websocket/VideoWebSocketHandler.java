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
                    if (yoloSession != null && yoloSession.isOpen()) {
                        Thread.sleep(5000); // 연결 유지는 주기적으로 확인
                        continue;
                    }

                    System.out.println("🔄 YOLO 서버에 연결 시도 중...");
                    WebSocketClient client = new StandardWebSocketClient();
                    client.doHandshake(new TextWebSocketHandler() {
                        @Override
                        public void afterConnectionEstablished(WebSocketSession session) {
                            yoloSession = session;
                            System.out.println("✅ YOLO 서버와 연결됨");
                        }

                        @Override
                        public void handleTransportError(WebSocketSession session, Throwable exception) {
                            System.out.println("❌ 전송 오류 발생: " + exception.getMessage());
                            yoloSession = null;
                        }

                        @Override
                        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                            System.out.println("❌ 연결 종료됨: " + status);
                            yoloSession = null;
                        }

                    }, "ws://15.165.114.170:8765/ws/fall");

                    // 연결 후 대기
                    Thread.sleep(5000);
                } catch (Exception e) {
                    System.out.println("🚨 YOLO 서버 연결 실패: " + e.getMessage());
                    try {
                        Thread.sleep(3000); // 3초 후 재시도
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
