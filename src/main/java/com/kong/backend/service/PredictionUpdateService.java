package com.kong.backend.service;

import com.kong.backend.DTO.PredictionUpdateRequest;
import com.kong.backend.DTO.PredictionUpdateResponse;
import com.kong.backend.Entity.PredictionEntity;
import com.kong.backend.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class PredictionUpdateService {

    private final PredictionRepository repo;

    /** 여러 개를 한 번에 부분 수정 (단일 수정 없음) */
    @Transactional
    public PredictionUpdateResponse update(PredictionUpdateRequest req) {
        var results = new ArrayList<PredictionUpdateResponse.Result>();
        int updated = 0, skipped = 0, failed = 0;

        for (var item : req.updates()) {
            try {
                PredictionEntity p = repo.findById(item.predKey())
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 predKey=" + item.predKey()));

                // 모두 같은 paKey인지 검증
                if (!p.getPatient().getPaKey().equals(req.paKey())) {
                    skipped++;
                    results.add(new PredictionUpdateResponse.Result(
                            item.predKey(), "skipped", "요청 paKey와 레코드 paKey 불일치"
                    ));
                    continue;
                }

                // 새 값 계산 (null이면 미수정)
                String newDisease = item.diseaseCode() != null ? item.diseaseCode() : p.getDiseaseCode();
                Double newRisk    = item.riskScore()  != null ? item.riskScore()  : p.getRiskScore();
                var newPredAt     = item.predictedAt()!= null ? item.predictedAt(): p.getPredictedAt();

                // 변경 없으면 skip
                if (newDisease.equals(p.getDiseaseCode())
                        && newRisk.equals(p.getRiskScore())
                        && newPredAt.equals(p.getPredictedAt())) {
                    skipped++;
                    results.add(new PredictionUpdateResponse.Result(
                            item.predKey(), "skipped", "변경 사항 없음"
                    ));
                    continue;
                }

                // 유니크 충돌 검사
                boolean dup = repo.existsByPatient_PaKeyAndDiseaseCodeAndPredictedAtAndPredKeyNot(
                        req.paKey(), newDisease, newPredAt, p.getPredKey()
                );
                if (dup) {
                    failed++;
                    results.add(new PredictionUpdateResponse.Result(
                            item.predKey(), "failed",
                            "중복 충돌: (paKey=%d, diseaseCode=%s, predictedAt=%s)".formatted(
                                    req.paKey(), newDisease, newPredAt)
                    ));
                    continue;
                }

                // 변경 반영
                p.setDiseaseCode(newDisease);
                p.setRiskScore(newRisk);
                p.setPredictedAt(newPredAt);

                updated++;
                results.add(new PredictionUpdateResponse.Result(
                        item.predKey(), "updated", "수정 완료"
                ));
            } catch (IllegalArgumentException e) {
                failed++;
                results.add(new PredictionUpdateResponse.Result(
                        item.predKey(), "failed", e.getMessage()
                ));
            } catch (Exception e) {
                failed++;
                results.add(new PredictionUpdateResponse.Result(
                        item.predKey(), "failed", "예상치 못한 오류: " + e.getMessage()
                ));
            }
        }

        return new PredictionUpdateResponse(
                req.paKey(), req.updates().size(), updated, skipped, failed, results
        );
    }
}
