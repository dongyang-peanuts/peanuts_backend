package com.kong.backend.repository;

import com.kong.backend.Entity.PredictionEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PredictionRepository
        extends JpaRepository<PredictionEntity, Integer>, JpaSpecificationExecutor<PredictionEntity> {

    boolean existsByPatient_PaKeyAndDiseaseCodeAndPredictedAt(
            Integer paKey, String diseaseCode, java.time.Instant predictedAt);

    // ✅ 수정 시 자기 자신(predKey)은 제외하고 중복 여부 확인
    boolean existsByPatient_PaKeyAndDiseaseCodeAndPredictedAtAndPredKeyNot(
            Integer paKey, String diseaseCode, java.time.Instant predictedAt, Integer predKey);

    // ✅ MySQL 8+/MariaDB 10.2+ : 모든 환자에 대해 예측 상위 4개씩 한 번에 가져오기 (N+1 회피)
    @Query(value = """
        SELECT p.*
        FROM prediction p
        JOIN (
            SELECT predKey,
                   ROW_NUMBER() OVER (PARTITION BY paKey ORDER BY predictedAt DESC) AS rn
            FROM prediction
        ) x ON x.predKey = p.predKey
        WHERE x.rn <= 4
        """, nativeQuery = true)
    List<PredictionEntity> findTop4GroupByPatientForAll();

    // ⏪ (대안) 특정 환자만 상위 4개 조회가 필요할 때 쓸 수 있는 메서드 (N+1 버전에서 사용)
    @Query("""
           SELECT p FROM PredictionEntity p
           WHERE p.patient.paKey = :paKey
           ORDER BY p.predictedAt DESC
           """)
    List<PredictionEntity> findTop4ByPaKeyOrderByPredictedAtDesc(@Param("paKey") Integer paKey,
                                                                 org.springframework.data.domain.Pageable pageable);
}
