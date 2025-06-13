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
                protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                    String payload = message.getPayload();
                    System.out.println("📥 YOLO 서버 결과 수신: " + payload);

                    try {
                        JsonNode json = mapper.readTree(payload);

                        // userKey는 고정값 1로 설정
                        int userKey = 1;

                        String alertLevel = json.get("alertLevel").asText();
                        String eventType = json.get("eventType").asText();
                        LocalDateTime detectedAt = LocalDateTime.parse(json.get("detectedAt").asText());

                        // DB 저장
                        alertService.saveAlert(alertLevel, eventType, detectedAt, userKey);

                    } catch (Exception e) {
                        System.out.println("❌ 파싱 또는 저장 실패: " + e.getMessage());
                    }

                    // 관리자에게 결과 전달
                    for (WebSocketSession admin : adminSessions) {
                        if (admin.isOpen()) {
                            admin.sendMessage(message);
                        }
                    }

                    // 사용자에게 결과 전달
                    for (WebSocketSession user : userSessions) {
                        if (user.isOpen()) {
                            user.sendMessage(message);
                        }
                    }
                }
            }, "ws://106.101.8.112/ws/fall");

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
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        for (WebSocketSession admin : adminSessions) {
            if (admin.isOpen()) {
                admin.sendMessage(message);
            }
        }

        for (WebSocketSession user : userSessions) {
            if (user.isOpen() && !user.getId().equals(session.getId())) {
                user.sendMessage(message);
            }
        }

        if (yoloSession != null && yoloSession.isOpen()) {
            yoloSession.sendMessage(message);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        userSessions.remove(session);
        adminSessions.remove(session);
    }
}
