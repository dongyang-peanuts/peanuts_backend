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

import java.io.IOException;
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

                        int userKey = 1; // 임시 유저 식별자
                        String alertLevel = json.get("alertLevel").asText();
                        String eventType = json.get("eventType").asText();
                        LocalDateTime detectedAt = LocalDateTime.parse(json.get("detectedAt").asText());

                        alertService.saveAlert(alertLevel, eventType, detectedAt, userKey);
                    } catch (Exception e) {
                        System.out.println("❌ 파싱 또는 저장 실패: " + e.getMessage());
                    }

                    // 관리자에게 전송
                    for (WebSocketSession admin : adminSessions) {
                        try {
                            if (admin.isOpen()) {
                                admin.sendMessage(message);
                            }
                        } catch (IOException e) {
                            System.out.println("❌ 관리자 전송 실패: " + e.getMessage());
                        }
                    }

                    // 사용자에게 전송
                    for (WebSocketSession user : userSessions) {
                        try {
                            if (user.isOpen()) {
                                user.sendMessage(message);
                            }
                        } catch (IOException e) {
                            System.out.println("❌ 사용자 전송 실패: " + e.getMessage());
                        }
                    }
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
        for (WebSocketSession admin : adminSessions) {
            try {
                if (admin.isOpen()) {
                    admin.sendMessage(message);
                }
            } catch (IOException e) {
                System.out.println("❌ 관리자 전송 실패: " + e.getMessage());
            }
        }

        for (WebSocketSession user : userSessions) {
            try {
                if (user.isOpen() && !user.getId().equals(session.getId())) {
                    user.sendMessage(message);
                }
            } catch (IOException e) {
                System.out.println("❌ 사용자 전송 실패: " + e.getMessage());
            }
        }

        if (yoloSession != null && yoloSession.isOpen()) {
            try {
                yoloSession.sendMessage(message);
            } catch (IOException e) {
                System.out.println("❌ YOLO 서버로 전송 실패: " + e.getMessage());
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        userSessions.remove(session);
        adminSessions.remove(session);
    }
}
