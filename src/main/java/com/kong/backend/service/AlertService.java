package com.kong.backend.service;

import com.kong.backend.DTO.AlertCreateTestDto;
import com.kong.backend.DTO.AlertHistoryDto;
import com.kong.backend.Entity.AlertHistoryEntity;
import com.kong.backend.Entity.UserEntity;
import com.kong.backend.Entity.VideoEntity;
import com.kong.backend.exception.UserNotFoundException;
import com.kong.backend.repository.AlertHistoryRepository;
import com.kong.backend.repository.UserRepository;
import com.kong.backend.repository.VideoRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final UserRepository userRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final VideoRepository videoRepository;

    /* ------------------------------
     * 조회: 유저별(페이징)
     * ------------------------------ */
    @Transactional(readOnly = true)
    public Page<AlertHistoryDto> getAlertsByUserKeyPaged(Integer userKey, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "detectedAt"));
        Page<AlertHistoryEntity> alerts = alertHistoryRepository.findByUser_UserKey(userKey, pageable);
        return alerts.map(this::toDto);
    }

    /* ------------------------------
     * 조회: 전체(페이징)
     * ------------------------------ */
    @Transactional(readOnly = true)
    public Page<AlertHistoryDto> getAllAlertsPaged(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "detectedAt"));
        Page<AlertHistoryEntity> alerts = alertHistoryRepository.findAll(pageable);
        return alerts.map(this::toDto);
    }

    /* ------------------------------
     * 조회: 유저별(리스트, 기존 유지)
     * ------------------------------ */
    @Transactional(readOnly = true)
    public List<AlertHistoryDto> getAlertHistoryByUserKey(Integer userKey) {
        var alerts = alertHistoryRepository.findByUser_UserKeyOrderByDetectedAtDesc(userKey);
        return alerts.stream().map(this::toDto).toList();
    }

    /* ------------------------------
     * 조회: 전체(리스트, 기존 유지)
     * ------------------------------ */
    @Transactional(readOnly = true)
    public List<AlertHistoryDto> getAllAlertHistories() {
        var alerts = alertHistoryRepository.findAll(Sort.by(Sort.Direction.DESC, "detectedAt"));
        return alerts.stream().map(this::toDto).toList();
    }

    /* ------------------------------
     * 더미 저장 (기존 유지)
     * ------------------------------ */
    @Transactional
    public void saveMultipleTestAlerts(List<AlertCreateTestDto> dtos) {
        for (AlertCreateTestDto dto : dtos) {
            UserEntity user = userRepository.findById(dto.getUserKey())
                    .orElseThrow(() -> new UserNotFoundException("유저 없음: " + dto.getUserKey()));

            AlertHistoryEntity alert = new AlertHistoryEntity();
            alert.setUser(user);
            alert.setEventType(dto.getEventType());
            alert.setDetectedAt(LocalDateTime.now());

            alertHistoryRepository.save(alert);
        }
    }

    /* ------------------------------
     * 새 스키마 대응 저장 (권장) - videoId 로 VideoEntity FK 연결
     * detectedAt 이 null이면 ts(epoch) 또는 서버시간으로 보정
     * ------------------------------ */
    @Transactional
    public AlertHistoryDto saveAlert(SaveAlertCommand cmd) {
        UserEntity user = userRepository.findById(cmd.getUserKey())
                .orElseThrow(() -> new UserNotFoundException("유저 없음: " + cmd.getUserKey()));

        // detectedAt 보정
        LocalDateTime detectedAt = cmd.getDetectedAt();
        if (detectedAt == null) {
            if (cmd.getTs() != null) {
                detectedAt = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli((long) (cmd.getTs() * 1000)), KST
                );
            } else {
                detectedAt = LocalDateTime.now();
            }
        }

        AlertHistoryEntity alert = new AlertHistoryEntity();
        alert.setUser(user);
        alert.setEventType(cmd.getEventType());
        alert.setDetectedAt(detectedAt);
        alert.setLayRate(normalizeLayRate(cmd.getLayRate()));
        alert.setProb(cmd.getProb());
        alert.setTs(cmd.getTs());

        // ✅ Video FK 연결 (선택)
        if (cmd.getVideoId() != null) {
            VideoEntity video = videoRepository.findById(cmd.getVideoId())
                    .orElseThrow(() -> new IllegalArgumentException("비디오 없음: " + cmd.getVideoId()));
            alert.setVideo(video);
        }

        AlertHistoryEntity saved = alertHistoryRepository.save(alert);
        log.info("✅ 알림 저장: type={}, userKey={}, detectedAt={}, videoId={}",
                saved.getEventType(),
                user.getUserKey(),
                saved.getDetectedAt(),
                saved.getVideo() != null ? saved.getVideo().getVideoId() : null
        );

        return toDto(saved);
    }

    /* ------------------------------
     * 레거시 호환 시그니처 (videoPath 제거, videoId 없이 저장)
     * 필요 시 라즈베리파이/핸들러 구버전에서 호출
     * ------------------------------ */
    @Transactional
    public AlertHistoryDto saveAlert(
            String eventType,
            LocalDateTime detectedAt,
            int userKey,
            Double layRate,
            Double prob,
            Double ts
    ) {
        return saveAlert(SaveAlertCommand.builder()
                .eventType(eventType)
                .detectedAt(detectedAt)
                .userKey(userKey)
                .layRate(layRate)
                .prob(prob)
                .ts(ts)
                .build());
    }

    /* ------------------------------
     * 매퍼/유틸
     * ------------------------------ */
    private AlertHistoryDto toDto(AlertHistoryEntity e) {
        AlertHistoryDto dto = new AlertHistoryDto();
        dto.setAlertId(e.getAlertId());
        dto.setEventType(e.getEventType());
        dto.setDetectedAt(e.getDetectedAt());
        dto.setLayRate(e.getLayRate());
        dto.setProb(e.getProb());
        dto.setTs(e.getTs());
        // 필요 시 DTO에 videoId 필드가 있다면 아래 주석을 해제
        // if (e.getVideo() != null) dto.setVideoId(e.getVideo().getVideoId());
        return dto;
    }

    private Double normalizeLayRate(Double layRate) {
        if (layRate == null) return null;
        double v = layRate;
        if (Double.isNaN(v) || Double.isInfinite(v)) return null;
        if (v < 0.0) v = 0.0;
        if (v > 1.0) v = 1.0;
        return v;
    }

    /* ------------------------------
     * 커맨드 객체
     * ------------------------------ */
    @Data
    @Builder
    public static class SaveAlertCommand {
        private String eventType;
        private LocalDateTime detectedAt; // nullable
        private int userKey;
        private Double layRate;            // nullable
        private Double prob;               // nullable
        private Double ts;                 // nullable (epoch seconds, float)
        private Long videoId;              // nullable (VideoEntity FK)
    }
}
