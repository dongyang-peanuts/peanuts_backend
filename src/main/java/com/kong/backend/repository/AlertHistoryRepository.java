package com.kong.backend.repository;

import com.kong.backend.Entity.AlertHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertHistoryRepository extends JpaRepository<AlertHistoryEntity, Integer> {

    // 사용자별 최근 알림(전부)
    List<AlertHistoryEntity> findByUser_UserKeyOrderByDetectedAtDesc(Integer userKey);

    // 사용자별 최근 상위 N건 (Pageable로 N개 제한)
    List<AlertHistoryEntity> findByUser_UserKeyOrderByDetectedAtDesc(Integer userKey, Pageable pageable);

    // 전체 최근 상위 N건
    List<AlertHistoryEntity> findAllByOrderByDetectedAtDesc(Pageable pageable);

    // 특정 기간 내 사용자 알림 (최근순)
    List<AlertHistoryEntity> findByUser_UserKeyAndDetectedAtBetweenOrderByDetectedAtDesc(
            Integer userKey, LocalDateTime start, LocalDateTime end
    );

    // 이벤트 타입별 (예: "낙상", "낙상 해제" 등)
    List<AlertHistoryEntity> findByUser_UserKeyAndEventTypeOrderByDetectedAtDesc(
            Integer userKey, String eventType
    );

    // 확률 기준 (prob >= threshold) – 필요 시 사용
    List<AlertHistoryEntity> findByUser_UserKeyAndProbGreaterThanEqualOrderByDetectedAtDesc(
            Integer userKey, Double prob
    );

    Page<AlertHistoryEntity> findByUser_UserKey(Integer userKey, Pageable pageable);

}
