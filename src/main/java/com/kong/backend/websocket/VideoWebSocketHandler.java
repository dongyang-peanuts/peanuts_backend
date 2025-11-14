package com.kong.backend.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kong.backend.service.AlertService;
import com.kong.backend.service.AlertService.SaveAlertCommand;
import com.kong.backend.service.DeviceControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoWebSocketHandler extends TextWebSocketHandler {

    private final AlertService alertService;
    private final DeviceControlService deviceControlService;
    private final ObjectMapper mapper = new ObjectMapper();

    // WebSocket 세션 그룹
    private final Set<WebSocketSession> videoSessions  = ConcurrentHashMap.newKeySet(); // 영상 미러링
    private final Set<WebSocketSession> alertSessions  = ConcurrentHashMap.newKeySet(); // 관리자 알림
    private static final int MAX_ALERT_SESSIONS = 5; // 세션 최대 제한

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /** 연결 수립 */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String path = session.getUri() != null ? session.getUri().getPath() : "";
        try {
            // 중복 연결 방지 (IP 기준)
            if (session.getRemoteAddress() != null) {
                alertSessions.removeIf(s -> s.getRemoteAddress().equals(session.getRemoteAddress()));
            }

            if (path.contains("/ws/video") || path.contains("/ws/admin/monitor")) {
                videoSessions.add(session);
                log.info("✅ 영상 채널 연결: {}", path);
            } else if (path.contains("/ws/alert")) {
                if (alertSessions.size() >= MAX_ALERT_SESSIONS) {
                    log.warn("🚫 알림 세션 초과({}) → 연결 거부", MAX_ALERT_SESSIONS);
                    session.close(CloseStatus.SERVICE_OVERLOAD);
                    return;
                }
                alertSessions.add(session);
                log.info("✅ 알림 채널 연결: {} (현재 {}개)", path, alertSessions.size());
            } else if (path.contains("/ws/fall")) {
                deviceControlService.registerDevice(session);
                log.info("✅ 디바이스 채널 연결: {}", path);
            } else {
                log.info("ℹ️ 알 수 없는 경로 연결: {}", path);
            }
        } catch (Exception e) {
            log.error("❌ afterConnectionEstablished 처리 오류", e);
        }
    }

    /** 텍스트 메시지 처리 */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String path = session.getUri() != null ? session.getUri().getPath() : "";
        try {
            if (path.contains("/ws/fall")) {
                onDeviceEvent(message.getPayload());
                broadcastSafe(alertSessions, message);
            } else if (path.contains("/ws/video") || path.contains("/ws/admin/monitor")) {
                broadcastSafe(videoSessions, message);
            }
        } catch (Exception e) {
            log.error("❌ 텍스트 메시지 처리 오류", e);
        }
    }

    /** 세션 종료 */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        videoSessions.remove(session);
        alertSessions.remove(session);
        deviceControlService.unregisterDevice(session);
        log.info("🔻 세션 종료: {} (alert={}, video={})",
                status.getReason(), alertSessions.size(), videoSessions.size());
    }

    // ===============================================================
    //               라즈베리파이 이벤트 수신 및 전송
    // ===============================================================

    private void onDeviceEvent(String payload) {
        try {
            JsonNode json = mapper.readTree(payload);
            String eventType = getTextOrNull(json, "eventType");
            Double layRate   = getDoubleOrNull(json, "layRate");
            Double prob      = getDoubleOrNull(json, "prob");
            Double ts        = getDoubleOrNull(json, "ts");
            Integer videoId  = getIntegerOrNull(json, "videoId");

            if (eventType == null || ts == null) {
                log.warn("❌ 필수 필드 누락(eventType/ts): {}", payload);
                return;
            }

            LocalDateTime detectedAt = tsToLocalDateTime(ts, KST);
            int userKey = resolveUserKey(json);

            var savedDto = alertService.saveAlert(
                    SaveAlertCommand.builder()
                            .eventType(eventType)
                            .detectedAt(detectedAt)
                            .userKey(userKey)
                            .layRate(layRate)
                            .prob(prob)
                            .ts(ts)
                            .videoId(videoId)
                            .build()
            );

            String enriched = enrichPayloadForClients(json, detectedAt, userKey, eventType, savedDto.getAlertId(), videoId);

            broadcastSafe(alertSessions, new TextMessage(enriched));              // 관리자 알림
            deviceControlService.broadcastToDevices(new TextMessage(enriched));   // 디바이스 브로드캐스트

        } catch (Exception e) {
            log.error("❌ 디바이스 이벤트 처리 실패", e);
        }
    }

    private String enrichPayloadForClients(JsonNode original,
                                           LocalDateTime detectedAt,
                                           int userKey,
                                           String eventType,
                                           Integer alertId,
                                           Integer videoId) {
        ObjectNode node = original.deepCopy();
        node.put("detectedAtIso", detectedAt.format(TS_FMT));
        node.put("userKey", userKey);
        node.put("eventType", eventType);
        if (alertId != null) node.put("alertId", alertId);
        if (videoId != null) node.put("videoId", videoId);
        return node.toString();
    }

    // ===============================================================
    //                       브로드캐스트 안전 처리
    // ===============================================================

    private void broadcastSafe(Set<WebSocketSession> sessions, TextMessage message) {
        sessions.removeIf(s -> !s.isOpen()); // 닫힌 세션 정리
        for (WebSocketSession s : sessions) {
            try {
                if (s.isOpen()) s.sendMessage(message);
            } catch (Exception e) {
                log.warn("⚠️ 세션 전송 실패 → 제거: {}", s.getId());
                try { s.close(); } catch (Exception ignore) {}
            }
        }
    }

    // ===============================================================
    //                       주기적 정리 (10초마다)
    // ===============================================================

    @Scheduled(fixedRate = 10000)
    public void cleanupClosedSessions() {
        int before = alertSessions.size();
        alertSessions.removeIf(s -> !s.isOpen());
        int after = alertSessions.size();
        if (before != after) {
            log.info("🧹 닫힌 세션 정리: {} → {}", before, after);
        }
    }

    // ===============================================================
    //                         헬퍼 함수
    // ===============================================================

    private String getTextOrNull(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    private Double getDoubleOrNull(JsonNode node, String field) {
        return node.hasNonNull(field) && node.get(field).isNumber()
                ? node.get(field).asDouble() : null;
    }

    private Integer getIntegerOrNull(JsonNode node, String field) {
        return node.hasNonNull(field) && node.get(field).canConvertToInt()
                ? node.get(field).asInt() : null;
    }

    private LocalDateTime tsToLocalDateTime(double epochSeconds, ZoneId zoneId) {
        long seconds = (long) epochSeconds;
        long nanos = (long) ((epochSeconds - seconds) * 1_000_000_000L);
        return Instant.ofEpochSecond(seconds, nanos).atZone(zoneId).toLocalDateTime();
    }

    private int resolveUserKey(JsonNode json) {
        return 1; // JWT 기반 추후 확장 가능
    }
}
