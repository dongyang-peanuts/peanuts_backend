package com.kong.backend.repository;

import com.kong.backend.Entity.AlertHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertHistoryRepository extends JpaRepository<AlertHistoryEntity, Integer> {

    // 기본: 사용자별 최근 알림 조회
    List<AlertHistoryEntity> findByUser_UserKeyOrderByDetectedAtDesc(Integer userKey);

    // 추가 예시들 ↓

    // 특정 기간 내 사용자 알림 조회
    List<AlertHistoryEntity> findByUser_UserKeyAndDetectedAtBetweenOrderByDetectedAtDesc(
            Integer userKey, LocalDateTime start, LocalDateTime end
    );

    // 낙상(fall=true) 이벤트만 조회
    List<AlertHistoryEntity> findByUser_UserKeyAndFallTrueOrderByDetectedAtDesc(Integer userKey);

    // 특정 포즈 상태만 조회
    List<AlertHistoryEntity> findByPoseOrderByDetectedAtDesc(String pose);

    // 알림 레벨별 조회
    List<AlertHistoryEntity> findByAlertLevelOrderByDetectedAtDesc(String alertLevel);
}
