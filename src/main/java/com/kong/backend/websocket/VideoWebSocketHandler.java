package com.kong.backend.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kong.backend.service.AlertService;
import com.kong.backend.service.DeviceControlService;
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

    // 세션 보관 (브라우저 쪽만 유지)
    private final Set<WebSocketSession> videoSessions  = ConcurrentHashMap.newKeySet(); // 사용자/관리자 영상 미러링
    private final Set<WebSocketSession> alertSessions  = ConcurrentHashMap.newKeySet(); // 관리자 알림 구독

    // ✅ 디바이스 세션은 서비스로 위임 (REST로도 제어 가능하게)
    private final DeviceControlService deviceControlService;

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
        } else if (path.contains("/ws/fall")) { // 라즈베리파이(파이썬) 낙상/배회 이벤트 푸시
            deviceControlService.registerDevice(session);
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
                onDeviceEvent(message.getPayload());
            } else if (path.contains("/ws/video") || path.contains("/ws/admin/monitor")) {
                broadcastTo(videoSessions, message);
            } else if (path.contains("/ws/alert")) {
                // 일반적으로 수신 없음
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
        deviceControlService.unregisterDevice(session);
    }

    // ===================== 디바이스 이벤트 처리 =====================

    /** 라즈베리파이가 보내는 새 스키마(JSON) 처리 */
    private void onDeviceEvent(String payload) {
        System.out.println("📥 디바이스 이벤트 수신: " + payload);
        try {
            JsonNode json = mapper.readTree(payload);

            String eventType      = getTextOrNull(json, "eventType");
            Boolean fall          = getBooleanOrNull(json, "fall");
            Double layRate        = getDoubleOrNull(json, "layRate");
            String wanderState    = getTextOrNull(json, "wanderState");   // NORMAL / WANDERING
            Double wanderProb     = getDoubleOrNull(json, "wanderProb");  // 0.0 ~ 1.0
            String wanderPosture  = getTextOrNull(json, "wanderPosture"); // LYING / STANDING ...
            Double ts             = getDoubleOrNull(json, "ts");

            // 필수값 검증
            if (eventType == null || fall == null || layRate == null || ts == null) {
                System.out.println("❌ 필수 필드 누락: " + payload);
                return;
            }

            // 1) 항상 저장
            LocalDateTime detectedAt = tsToLocalDateTime(ts, KST);
            int userKey = resolveUserKey(json);

            String alertLevel = classifyAlertLevel(eventType, layRate, fall, wanderState, wanderProb);

            // pose 자리에 wanderPosture 매핑(시그니처 유지)
            alertService.saveAlert(
                    alertLevel,
                    eventType,
                    detectedAt,
                    userKey,
                    /* pose */ wanderPosture,
                    layRate,
                    fall,
                    ts,
                    null
            );

            // 2) 클라이언트(관리자 알림 + 디바이스) 모두에 항상 브로드캐스트
            String enriched = enrichPayloadForClients(json, alertLevel, detectedAt, userKey);
            broadcastTo(alertSessions, new TextMessage(enriched));                // 관리자
            deviceControlService.broadcastToDevices(new TextMessage(enriched));   // 디바이스

            System.out.println("✅ 저장/전송 완료 [" + alertLevel + "] " + eventType +
                    " @ " + detectedAt.format(TS_FMT));

        } catch (Exception e) {
            System.out.println("❌ 디바이스 이벤트 파싱 실패: " + e.getMessage());
        }
    }

    /** 클라이언트로 보낼 페이로드에 부가 정보 추가 */
    private String enrichPayloadForClients(JsonNode original,
                                           String alertLevel,
                                           LocalDateTime detectedAt,
                                           int userKey) {
        ObjectNode node = original.deepCopy();
        node.put("alertLevel", alertLevel);
        node.put("detectedAtIso", detectedAt.format(TS_FMT));
        node.put("userKey", userKey);
        return node.toString();
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

    /** 낙상/배회 혼합 스키마 대응 알림 레벨 분류 (레벨 산정만 유지) */
    private String classifyAlertLevel(String eventType,
                                      double layRate,
                                      boolean fall,
                                      String wanderState,
                                      Double wanderProb) {
        if ("낙상 해제".equals(eventType)) return "RECOVERY";
        if (fall) return "HIGH";
        boolean wanderingHigh =
                "WANDERING".equalsIgnoreCase(String.valueOf(wanderState)) &&
                        wanderProb != null && wanderProb >= 0.80;
        if (wanderingHigh) return "MEDIUM";
        if (layRate > 0.6) return "MEDIUM";
        if (layRate > 0.3) return "LOW";
        return "INFO";
    }

    /** 실제 서비스에선 JWT/HandshakeInterceptor로 userKey를 세션에 넣고 꺼내세요. */
    private int resolveUserKey(JsonNode json) {
        // TODO: 메시지 또는 session attributes에서 꺼내도록 확장
        return 1;
    }
}
