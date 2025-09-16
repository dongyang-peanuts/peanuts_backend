package com.kong.backend.service;

import com.kong.backend.DTO.AlertCreateTestDto;
import com.kong.backend.DTO.AlertHistoryDto;
import com.kong.backend.Entity.AlertHistoryEntity;
import com.kong.backend.Entity.UserEntity;
import com.kong.backend.exception.UserNotFoundException;
import com.kong.backend.repository.AlertHistoryRepository;
import com.kong.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final UserRepository userRepository;
    private final AlertHistoryRepository alertHistoryRepository;

    /* ------------------------------
     * 조회 영역
     * ------------------------------ */
    @Transactional(readOnly = true)
    public List<AlertHistoryDto> getAlertHistoryByUserKey(Integer userKey) {
        List<AlertHistoryEntity> alerts =
                alertHistoryRepository.findByUser_UserKeyOrderByDetectedAtDesc(userKey);

        return alerts.stream().map(entity -> {
            AlertHistoryDto dto = new AlertHistoryDto();
            dto.setAlertId(entity.getAlertId());
            dto.setEventType(entity.getEventType());
            dto.setAlertLevel(entity.getAlertLevel());
            dto.setDetectedAt(entity.getDetectedAt());

            // DTO가 확장되어 있다면 아래도 매핑하세요.
            dto.setPose(entity.getPose());
            dto.setLayRate(entity.getLayRate());
            dto.setFall(entity.getFall());
            dto.setTs(entity.getTs());

            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AlertHistoryDto> getAllAlertHistories() {
        List<AlertHistoryEntity> alerts =
                alertHistoryRepository.findAll(Sort.by(Sort.Direction.DESC, "detectedAt"));

        return alerts.stream().map(entity -> {
            AlertHistoryDto dto = new AlertHistoryDto();
            dto.setAlertId(entity.getAlertId());
            dto.setEventType(entity.getEventType());
            dto.setAlertLevel(entity.getAlertLevel());
            dto.setDetectedAt(entity.getDetectedAt());

            // DTO 확장 시 매핑
            dto.setPose(entity.getPose());
            dto.setLayRate(entity.getLayRate());
            dto.setFall(entity.getFall());
            dto.setTs(entity.getTs());

            return dto;
        }).collect(Collectors.toList());
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
            alert.setAlertLevel(dto.getAlertLevel());
            alert.setDetectedAt(LocalDateTime.now());

            alertHistoryRepository.save(alert);
        }
    }

    /* ------------------------------
     * 새 스키마 대응 저장 (권장 사용)
     * ------------------------------ */
    @Transactional
    public void saveAlert(String alertLevel,
                          String eventType,
                          LocalDateTime detectedAt,
                          int userKey,
                          String pose,            // stand | sit | lay | unknown
                          Double layRate,         // 0.0 ~ 1.0 (nullable)
                          Boolean fall,           // true | false (nullable)
                          Double ts,              // epoch seconds (nullable)
                          String videoPath) {

        try {
            UserEntity user = userRepository.findById(userKey)
                    .orElseThrow(() -> new UserNotFoundException("유저 없음: " + userKey));

            AlertHistoryEntity alert = new AlertHistoryEntity();
            alert.setUser(user);
            alert.setAlertLevel(alertLevel);
            alert.setEventType(eventType);
            alert.setDetectedAt(detectedAt);
            alert.setVideoPath(videoPath);

            // --- 추가 필드 ---
            alert.setPose(pose);
            alert.setLayRate(normalizeLayRate(layRate));
            alert.setFall(fall);
            alert.setTs(ts);

            alertHistoryRepository.save(alert);
            System.out.println("✅ 알림 저장 완료(확장): " + alertLevel + ", " + eventType + " @ " + detectedAt);
        } catch (Exception e) {
            System.out.println("❌ 알림 저장 실패(확장): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /* ------------------------------
     * 기존 시그니처 (호환용 래퍼)
     *  - 파이썬/핸들러 쪽 코드가 아직 구버전이면 이 메서드가 호출될 수 있습니다.
     *  - 가능한 빨리 위의 확장 시그니처로 전환하세요.
     * ------------------------------ */
    @Transactional
    public void saveAlert(String alertLevel,
                          String eventType,
                          LocalDateTime detectedAt,
                          int userKey,
                          String videoPath) {
        // 확장 메서드로 위임 (추가값은 null)
        saveAlert(alertLevel, eventType, detectedAt, userKey,
                null, null, null, null, videoPath);
    }

    /* ------------------------------
     * 유틸
     * ------------------------------ */
    private Double normalizeLayRate(Double layRate) {
        if (layRate == null) return null;
        double v = layRate;
        if (Double.isNaN(v) || Double.isInfinite(v)) return null;
        if (v < 0.0) v = 0.0;
        if (v > 1.0) v = 1.0;
        // 소수 4자리 정도로 제한하고 싶다면:
        // return Math.round(v * 10000.0) / 10000.0;
        return v;
    }
}
