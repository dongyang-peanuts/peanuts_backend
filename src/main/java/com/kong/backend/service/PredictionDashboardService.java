package com.kong.backend.service;

import com.kong.backend.DTO.PatientWithPredictionsResponse;
import com.kong.backend.DTO.PredictionDto;
import com.kong.backend.Entity.PatientEntity;
import com.kong.backend.Entity.PredictionEntity;
import com.kong.backend.repository.PatientRepository;
import com.kong.backend.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PredictionDashboardService {

    private final PatientRepository patientRepo;
    private final PredictionRepository predictionRepo;

    /**
     * 전체 환자 + 환자별 최신 예측 4건 (윈도우 함수 최적화 버전)
     */
    public List<PatientWithPredictionsResponse> getAllPatientsWithLatestPredictions() {
        // 1) 모든 환자 로드
        List<PatientEntity> patients = patientRepo.findAll();

        if (patients.isEmpty()) {
            return List.of();
        }

        // 2) 윈도우 함수로 모든 환자의 상위 4개 예측 한 번에 조회
        List<PredictionEntity> topPredictions = predictionRepo.findTop4GroupByPatientForAll();

        // 3) paKey -> List<PredictionDto> 매핑
        Map<Integer, List<PredictionDto>> byPatient = topPredictions.stream()
                .map(this::toDto)
                .collect(Collectors.groupingBy(PredictionDto::paKey,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .sorted(Comparator.comparing(PredictionDto::predictedAt).reversed())
                                        .limit(4) // 안전빵으로 한 번 더 4개 제한
                                        .toList()
                        )));

        // 4) 환자 리스트를 응답으로 변환 (예측 없는 환자는 빈 리스트)
        return patients.stream()
                .map(p -> new PatientWithPredictionsResponse(
                        p.getPaKey(),
                        p.getPaName(),
                        p.getPaAddr(),
                        p.getPaAge(),
                        p.getPaHei(),
                        p.getPaWei(),
                        byPatient.getOrDefault(p.getPaKey(), List.of())
                ))
                .toList();
    }

    /**
     * ⏪ 윈도우 함수가 불가능할 때(구버전 DB 등) 사용할 N+1 대안
     *  - 데이터가 적거나 당장 성능 이슈가 없을 때만 사용 권장
     */
    public List<PatientWithPredictionsResponse> getAllPatientsWithLatestPredictions_NPlus1() {
        List<PatientEntity> patients = patientRepo.findAll();
        var pageableTop4 = PageRequest.of(0, 4, Sort.by(Sort.Direction.DESC, "predictedAt"));

        return patients.stream()
                .map(p -> {
                    var preds = predictionRepo.findTop4ByPaKeyOrderByPredictedAtDesc(p.getPaKey(), pageableTop4)
                            .stream().map(this::toDto).toList();
                    return new PatientWithPredictionsResponse(
                            p.getPaKey(), p.getPaName(), p.getPaAddr(),
                            p.getPaAge(), p.getPaHei(), p.getPaWei(), preds
                    );
                })
                .toList();
    }

    private PredictionDto toDto(PredictionEntity p) {
        return new PredictionDto(
                p.getPredKey(),
                p.getPatient().getPaKey(),
                p.getDiseaseCode(),
                p.getRiskScore(),
                p.getPredictedAt(),
                p.getCreatedAt()
        );
    }
}
