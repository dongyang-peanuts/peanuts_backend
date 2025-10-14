package com.kong.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class DeviceControlService {

    private final ObjectMapper mapper = new ObjectMapper();

    // Pi(WebSocket) 세션 관리
    private final Set<WebSocketSession> deviceSessions = ConcurrentHashMap.newKeySet();

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public void registerDevice(WebSocketSession session) {
        deviceSessions.add(session);
    }

    public void unregisterDevice(WebSocketSession session) {
        deviceSessions.remove(session);
    }

    /** REST 요청 시 호출 → Pi로 SAVE_CLIP 명령 전송 */
    public SaveClipAck sendSaveClip(int paKey, int durationSec, int preBufferSec, int postBufferSec) throws Exception {
        String clipId = generateClipId(paKey);
        String uploadUrl = "/api/videos/upload/" + clipId;

        var root = mapper.createObjectNode();
        root.put("type", "SAVE_CLIP");
        root.put("clipId", clipId);
        root.put("durationSec", durationSec);
        root.put("preBufferSec", preBufferSec);
        root.put("postBufferSec", postBufferSec);
        root.put("paKey", paKey);
        root.put("uploadUrl", uploadUrl);

        String json = mapper.writeValueAsString(root);

        for (WebSocketSession s : deviceSessions) {
            if (s.isOpen()) {
                s.sendMessage(new TextMessage(json));
            }
        }

        return new SaveClipAck(clipId, uploadUrl, durationSec, preBufferSec, postBufferSec, paKey);
    }

    private String generateClipId(int paKey) {
        String ts = LocalDateTime.now(KST).format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String rand = UUID.randomUUID().toString().substring(0, 8);
        return (paKey > 0 ? ("pa" + paKey + "_") : "") + ts + "_" + rand;
    }

    public record SaveClipAck(
            String clipId, String uploadUrl, int durationSec, int preBufferSec, int postBufferSec, int paKey
    ) {}

    public void broadcastToDevices(TextMessage message) {
        for (WebSocketSession s : deviceSessions) {
            try {
                if (s.isOpen()) s.sendMessage(message);
            } catch (Exception ignored) {}
        }
    }
}