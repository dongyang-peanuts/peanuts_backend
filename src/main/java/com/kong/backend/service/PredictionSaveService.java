package com.kong.backend.service;

import com.kong.backend.DTO.PredictionSaveRequest;
import com.kong.backend.DTO.PredictionSaveResponse;
import com.kong.backend.Entity.PatientEntity;
import com.kong.backend.Entity.PredictionEntity;
import com.kong.backend.repository.PatientRepository;
import com.kong.backend.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class PredictionSaveService {

    private final PredictionRepository predictionRepo;
    private final PatientRepository patientRepo;

    private PatientEntity getPatient(Integer paKey) {
        return patientRepo.findById(paKey)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 환자 (paKey=" + paKey + ")"));
    }

    @Transactional
    public PredictionSaveResponse save(PredictionSaveRequest req) {
        var patient = getPatient(req.paKey());

        var results = new ArrayList<PredictionSaveResponse.ItemResult>();
        int created = 0, skipped = 0;

        for (var it : req.predictions()) {
            boolean exists = predictionRepo.existsByPatient_PaKeyAndDiseaseCodeAndPredictedAt(
                    req.paKey(), it.diseaseCode(), it.predictedAt()
            );

            if (exists) {
                skipped++;
                results.add(new PredictionSaveResponse.ItemResult(
                        null, it.diseaseCode(), "skipped", "이미 동일 데이터 존재"
                ));
                continue;
            }

            var saved = predictionRepo.save(
                    PredictionEntity.builder()
                            .patient(patient)
                            .diseaseCode(it.diseaseCode())
                            .riskScore(it.riskScore())
                            .predictedAt(it.predictedAt())
                            .build()
            );

            created++;
            results.add(new PredictionSaveResponse.ItemResult(
                    saved.getPredKey(), it.diseaseCode(), "created", "저장됨"
            ));
        }

        return new PredictionSaveResponse(
                req.paKey(), req.predictions().size(), created, skipped, results
        );
    }
}
