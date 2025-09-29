package com.kong.backend.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kong.backend.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class VideoWebSocketHandler extends TextWebSocketHandler {

    private final AlertService alertService;
    private final ObjectMapper mapper = new ObjectMapper();

    // 세션 보관
    private final Set<WebSocketSession> videoSessions  = ConcurrentHashMap.newKeySet(); // 사용자/관리자 영상 미러링
    private final Set<WebSocketSession> alertSessions  = ConcurrentHashMap.newKeySet(); // 관리자 알림 구독
    private final Set<WebSocketSession> deviceSessions = ConcurrentHashMap.newKeySet(); // Pi(낙상 감지) 연결

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /** 세션 오픈 시 경로별 등록 */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String path = session.getUri() != null ? session.getUri().getPath() : "";
        if (path.contains("/ws/video") || path.contains("/ws/admin/monitor")) {
            videoSessions.add(session);
            System.out.println("✅ 영상 채널 연결됨: " + path);
        } else if (path.contains("/ws/alert")) {
            alertSessions.add(session);
            System.out.println("✅ 알림 채널 연결됨: " + path);
        } else if (path.contains("/ws/fall")) { // 라즈베리파이(파이썬) 낙상 감지 이벤트 푸시
            deviceSessions.add(session);
            System.out.println("✅ 디바이스 채널 연결됨: " + path);
        } else {
            System.out.println("ℹ️ 알 수 없는 경로로 연결: " + path);
        }
    }

    /** 텍스트 메시지 처리 */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String path = session.getUri() != null ? session.getUri().getPath() : "";
        try {
            if (path.contains("/ws/fall")) {
                // 라즈베리파이가 보내는 이벤트(JSON) → 파싱/저장/브로드캐스트
                onDeviceEvent(message.getPayload());
            } else if (path.contains("/ws/video") || path.contains("/ws/admin/monitor")) {
                // 영상 프레임/메시지 미러링(필요 시 유지)
                broadcastTo(videoSessions, message);
            } else if (path.contains("/ws/alert")) {
                // 알림 채널로 들어오는 메시지는 일반적으로 없음(무시)
            } else {
                System.out.println("ℹ️ 처리되지 않은 경로 메시지: " + path);
            }
        } catch (Exception e) {
            System.out.println("❌ 메시지 처리 오류: " + e.getMessage());
        }
    }

    /** 이진(영상) 메시지도 미러링이 필요하면 지원 */
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String path = session.getUri() != null ? session.getUri().getPath() : "";
        try {
            if (path.contains("/ws/video") || path.contains("/ws/admin/monitor")) {
                broadcastBinaryTo(videoSessions, message);
            }
        } catch (Exception e) {
            System.out.println("❌ 바이너리 메시지 처리 오류: " + e.getMessage());
        }
    }

    /** 세션 종료 처리 */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        videoSessions.remove(session);
        alertSessions.remove(session);
        deviceSessions.remove(session);
    }

    // ===================== 디바이스 이벤트 처리 =====================

    /** 라즈베리파이가 보내는 새 스키마(JSON) 처리 */
    private void onDeviceEvent(String payload) {
        System.out.println("📥 디바이스 이벤트 수신: " + payload);
        try {
            JsonNode json = mapper.readTree(payload);

            String eventType = getTextOrNull(json, "eventType");
            String pose      = getTextOrNull(json, "pose");
            Double layRate   = getDoubleOrNull(json, "layRate");
            Boolean fall     = getBooleanOrNull(json, "fall");
            Double ts        = getDoubleOrNull(json, "ts");

            // 필수값 검증
            if (eventType == null || pose == null || layRate == null || fall == null || ts == null) {
                System.out.println("❌ 필수 필드 누락: " + payload);
                return;
            }

            // 1) 항상 저장
            LocalDateTime detectedAt = tsToLocalDateTime(ts, KST);
            String alertLevel = classifyAlertLevel(eventType, pose, layRate, fall);
            int userKey = resolveUserKey(json);

            alertService.saveAlert(
                    alertLevel, eventType, detectedAt, userKey,
                    pose, layRate, fall, ts, null
            );

            // 2) 관리자 알림 채널에는 항상 브로드캐스트(변경 없음)
            broadcastTo(alertSessions, new TextMessage(payload));

            // 3) 라즈베리파이(클라이언트)에는 fall == true 일 때만 전송
            if (Boolean.TRUE.equals(fall)) {
                // 원문 그대로 보내도 되고, 필요하면 경량 메시지로 변환해도 됨
                // 예) 최소 필드만 담은 경량 메시지 전송:
            /*
            ObjectNode out = mapper.createObjectNode();
            out.put("type", "FALL_ALERT");
            out.put("eventType", eventType);
            out.put("ts", ts);
            out.put("alertLevel", alertLevel);
            broadcastTo(deviceSessions, new TextMessage(mapper.writeValueAsString(out)));
            */
                // 여기선 간단히 원문을 그대로 전달
                broadcastTo(deviceSessions, new TextMessage(payload));
            } else {
                System.out.println("↪ fall=false → 라즈베리파이로는 전송하지 않음");
            }

            System.out.println("✅ 저장 완료 [" + alertLevel + "] " + eventType +
                    " @ " + detectedAt.format(TS_FMT));

        } catch (Exception e) {
            System.out.println("❌ 디바이스 이벤트 파싱 실패: " + e.getMessage());
        }
    }

    // ===================== 브로드캐스트 유틸 =====================

    private void broadcastTo(Set<WebSocketSession> sessions, TextMessage message) {
        for (WebSocketSession s : sessions) {
            try {
                if (s.isOpen()) s.sendMessage(message);
            } catch (Exception e) {
                System.out.println("❌ 클라이언트 전송 실패: " + e.getMessage());
            }
        }
    }

    private void broadcastBinaryTo(Set<WebSocketSession> sessions, BinaryMessage message) {
        for (WebSocketSession s : sessions) {
            try {
                if (s.isOpen()) s.sendMessage(message);
            } catch (Exception e) {
                System.out.println("❌ 바이너리 전송 실패: " + e.getMessage());
            }
        }
    }

    // ===================== 헬퍼 =====================

    private String getTextOrNull(JsonNode node, String field) {
        return (node.hasNonNull(field) && !node.get(field).isMissingNode())
                ? node.get(field).asText()
                : null;
    }

    private Double getDoubleOrNull(JsonNode node, String field) {
        return (node.hasNonNull(field) && node.get(field).isNumber())
                ? node.get(field).asDouble()
                : null;
    }

    private Boolean getBooleanOrNull(JsonNode node, String field) {
        return (node.hasNonNull(field) && node.get(field).isBoolean())
                ? node.get(field).asBoolean()
                : null;
    }

    private LocalDateTime tsToLocalDateTime(double epochSeconds, ZoneId zoneId) {
        long seconds = (long) epochSeconds;
        long nanos = (long) ((epochSeconds - seconds) * 1_000_000_000L);
        return Instant.ofEpochSecond(seconds, nanos).atZone(zoneId).toLocalDateTime();
    }

    private String classifyAlertLevel(String eventType, String pose, double layRate, boolean fall) {
        if ("낙상 해제".equals(eventType)) return "RECOVERY";
        if (fall) return "HIGH";
        if ("lay".equalsIgnoreCase(pose) && layRate > 0.6) return "MEDIUM";
        if (layRate > 0.3) return "LOW";
        return "INFO";
    }

    /** 실제 서비스에선 JWT/HandshakeInterceptor로 userKey를 세션에 넣고 꺼내세요. */
    private int resolveUserKey(JsonNode json) {
        // TODO: 메시지 또는 session attributes에서 꺼내도록 확장
        return 1;
    }
}
