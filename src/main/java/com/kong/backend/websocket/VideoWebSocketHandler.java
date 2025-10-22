package com.kong.backend.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kong.backend.service.AlertService;
import com.kong.backend.service.AlertService.SaveAlertCommand;
import com.kong.backend.service.DeviceControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final ObjectMapper mapper = new ObjectMapper();

    // 세션 보관 (브라우저/관리자)
    private final Set<WebSocketSession> videoSessions  = ConcurrentHashMap.newKeySet(); // 영상 미러링
    private final Set<WebSocketSession> alertSessions  = ConcurrentHashMap.newKeySet(); // 관리자 알림 구독

    // 디바이스 제어/브로드캐스트
    private final DeviceControlService deviceControlService;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /** 세션 오픈 시 경로별 등록 */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String path = session.getUri() != null ? session.getUri().getPath() : "";
        try {
            if (path.contains("/ws/video") || path.contains("/ws/admin/monitor")) {
                videoSessions.add(session);
                log.info("✅ 영상 채널 연결: {}", path);
            } else if (path.contains("/ws/alert")) {
                alertSessions.add(session);
                log.info("✅ 알림 채널 연결: {}", path);
            } else if (path.contains("/ws/fall")) { // 라즈베리파이(낙상/배회 이벤트)
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
            } else if (path.contains("/ws/video") || path.contains("/ws/admin/monitor")) {
                broadcastTo(videoSessions, message);
            } else if (path.contains("/ws/alert")) {
                // 일반적으로 수신 없음
            } else {
                log.debug("ℹ️ 처리되지 않은 경로 메시지: {}", path);
            }
        } catch (Exception e) {
            log.error("❌ 텍스트 메시지 처리 오류", e);
        }
    }

    /** 바이너리(영상) 메시지 처리 (필요 시) */
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String path = session.getUri() != null ? session.getUri().getPath() : "";
        try {
            if (path.contains("/ws/video") || path.contains("/ws/admin/monitor")) {
                broadcastBinaryTo(videoSessions, message);
            }
        } catch (Exception e) {
            log.error("❌ 바이너리 메시지 처리 오류", e);
        }
    }

    /** 세션 종료 처리 */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        try {
            videoSessions.remove(session);
            alertSessions.remove(session);
            deviceControlService.unregisterDevice(session);
        } catch (Exception e) {
            log.warn("세션 종료 처리 중 예외", e);
        }
    }

    // ===================== 디바이스 이벤트 처리 =====================

    /** 라즈베리파이가 보내는 새 스키마(JSON) 처리 */
    private void onDeviceEvent(String payload) {
        log.debug("📥 디바이스 이벤트 수신: {}", payload);
        try {
            JsonNode json = mapper.readTree(payload);

            String eventType = getTextOrNull(json, "eventType");
            Double layRate   = getDoubleOrNull(json, "layRate");
            Double prob      = getDoubleOrNull(json, "prob");
            Double ts        = getDoubleOrNull(json, "ts");
            Integer videoId     = getIntegerOrNull(json, "videoId"); // 선택: 있으면 FK 연결

            // 필수값 검증
            if (eventType == null || ts == null) {
                log.warn("❌ 필수 필드 누락(eventType/ts): {}", payload);
                return;
            }

            // 1) 저장 (detectedAt: ts 또는 서버시간)
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
                            .videoId(videoId)   // 있으면 비디오 FK 연결
                            .build()
            );

            // 2) 브로드캐스트(관리자/디바이스)
            String enriched = enrichPayloadForClients(json, detectedAt, userKey, eventType, savedDto.getAlertId(), videoId);
            broadcastTo(alertSessions, new TextMessage(enriched));               // 관리자
            deviceControlService.broadcastToDevices(new TextMessage(enriched));  // 디바이스

            log.info("✅ 저장/전송 완료: type={}, userKey={}, detectedAt={}, videoId={}",
                    eventType, userKey, detectedAt.format(TS_FMT), videoId);

        } catch (Exception e) {
            log.error("❌ 디바이스 이벤트 파싱/처리 실패", e);
        }
    }

    /** 클라이언트로 보낼 페이로드에 부가 정보 추가 */
    private String enrichPayloadForClients(JsonNode original,
                                           LocalDateTime detectedAt,
                                           int userKey,
                                           String eventType,
                                           Integer alertId,   // 저장된 alertId
                                           Integer videoId) {
        ObjectNode node = original.deepCopy();
        node.put("detectedAtIso", detectedAt.format(TS_FMT));
        node.put("userKey", userKey);
        node.put("eventType", eventType);
        if (alertId != null) node.put("alertId", alertId);
        if (videoId != null) node.put("videoId", videoId);
        return node.toString();
    }

    // ===================== 브로드캐스트 유틸 =====================

    private void broadcastTo(Set<WebSocketSession> sessions, TextMessage message) {
        for (WebSocketSession s : sessions) {
            try {
                if (s.isOpen()) s.sendMessage(message);
            } catch (Exception e) {
                log.warn("❌ 텍스트 전송 실패: {}", s.getId(), e);
            }
        }
    }

    private void broadcastBinaryTo(Set<WebSocketSession> sessions, BinaryMessage message) {
        for (WebSocketSession s : sessions) {
            try {
                if (s.isOpen()) s.sendMessage(message);
            } catch (Exception e) {
                log.warn("❌ 바이너리 전송 실패: {}", s.getId(), e);
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

    private Long getLongOrNull(JsonNode node, String field) {
        return (node.hasNonNull(field) && node.get(field).canConvertToLong())
                ? node.get(field).asLong()
                : null;
    }

    // 1) Integer용 파서 추가
    private Integer getIntegerOrNull(JsonNode node, String field) {
        return (node.hasNonNull(field) && node.get(field).canConvertToInt())
                ? node.get(field).asInt()
                : null;
    }


    private LocalDateTime tsToLocalDateTime(double epochSeconds, ZoneId zoneId) {
        long seconds = (long) epochSeconds;
        long nanos = (long) ((epochSeconds - seconds) * 1_000_000_000L);
        return Instant.ofEpochSecond(seconds, nanos).atZone(zoneId).toLocalDateTime();
    }

    /** 실제 서비스에선 JWT/HandshakeInterceptor로 userKey를 세션에 넣고 꺼내세요. */
    private int resolveUserKey(JsonNode json) {
        // TODO: 메시지 또는 session attributes에서 꺼내도록 확장
        return 1;
    }
}
