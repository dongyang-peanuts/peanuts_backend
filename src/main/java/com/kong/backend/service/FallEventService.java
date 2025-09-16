package com.kong.backend.service;

import com.kong.backend.DTO.*;
import com.kong.backend.Entity.FallEventEntity;
import com.kong.backend.Entity.PatientEntity;
import com.kong.backend.repository.PatientRepository;
import com.kong.backend.repository.FallEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class FallEventService {

    private final FallEventRepository fallRepo;
    private final PatientRepository patientRepo;

    private PatientEntity getPatient(Integer paKey) {
        return patientRepo.findById(paKey)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 환자 (paKey=" + paKey + ")"));
    }

    // ===== 저장 =====
    @Transactional
    public Integer save(Integer paKey, String severity, Instant detectedAt) {
        var patient = getPatient(paKey);
        if (fallRepo.existsByPatient_PaKeyAndDetectedAt(paKey, detectedAt)) {
            throw new IllegalStateException("중복 낙상 데이터: (paKey=%d, detectedAt=%s)".formatted(paKey, detectedAt));
        }
        var saved = fallRepo.save(
                FallEventEntity.builder()
                        .patient(patient)
                        .severity(severity)
                        .detectedAt(detectedAt)
                        .build()
        );
        return saved.getFallKey();
    }

    // ===== 업서트(있으면 업데이트, 없으면 생성) =====
    @Transactional
    public FallEventUpsertResponse saveOrUpdate(Integer paKey, String severity, Instant detectedAt) {
        var patient = getPatient(paKey);
        var existed = fallRepo.findByPatient_PaKeyAndDetectedAt(paKey, detectedAt).orElse(null);

        if (existed == null) {
            var saved = fallRepo.save(
                    FallEventEntity.builder()
                            .patient(patient)
                            .severity(severity)
                            .detectedAt(detectedAt)
                            .build()
            );
            return new FallEventUpsertResponse(saved.getFallKey(), "created", paKey, severity, detectedAt);
        } else {
            existed.setSeverity(severity);
            return new FallEventUpsertResponse(existed.getFallKey(), "updated", paKey, severity, detectedAt);
        }
    }

    // ===== 조회: 특정 환자 페이징(항상 4개) =====
    @Transactional(readOnly = true)
    public PageResponse<FallEventDto> findByPatientPaged(Integer paKey, Integer page, String sort) {
        int pageNum = (page == null || page < 0) ? 0 : page;
        int pageSize = 4;

        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(pageNum, pageSize, sortObj);

        var data = fallRepo.findByPatient_PaKey(paKey, pageable);
        var items = data.getContent().stream().map(this::toDto).toList();

        return new PageResponse<>(
                items,
                data.getNumber(),
                data.getSize(),
                data.getTotalElements(),
                data.getTotalPages(),
                data.isLast()
        );
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) return Sort.by(Sort.Direction.DESC, "detectedAt");
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        Sort.Direction dir = (parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, field);
    }

    private FallEventDto toDto(FallEventEntity e) {
        return new FallEventDto(
                e.getFallKey(),
                e.getPatient().getPaKey(),
                e.getSeverity(),
                e.getDetectedAt(),
                e.getCreatedAt()
        );
    }

    // ===== 여러 건 동시 부분 수정 =====
    @Transactional
    public FallEventUpdateResponse updateMany(FallEventUpdateRequest req) {
        var results = new ArrayList<FallEventUpdateResponse.Result>();
        int updated = 0, skipped = 0, failed = 0;

        for (var item : req.updates()) {
            try {
                var entity = fallRepo.findById(item.fallKey())
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 fallKey=" + item.fallKey()));

                // paKey 검증
                if (!entity.getPatient().getPaKey().equals(req.paKey())) {
                    skipped++;
                    results.add(new FallEventUpdateResponse.Result(
                            item.fallKey(), "skipped", "요청 paKey와 레코드 paKey 불일치"
                    ));
                    continue;
                }

                // 현재 값
                String newSeverity = item.severity()   != null ? item.severity()   : entity.getSeverity();
                Instant newDetectedAt = item.detectedAt()!= null ? item.detectedAt(): entity.getDetectedAt();

                // 변경 없음
                if (newSeverity.equals(entity.getSeverity())
                        && newDetectedAt.equals(entity.getDetectedAt())) {
                    skipped++;
                    results.add(new FallEventUpdateResponse.Result(
                            item.fallKey(), "skipped", "변경 사항 없음"
                    ));
                    continue;
                }

                // 중복 충돌: (paKey, detectedAt) 자기 자신 제외 중복?
                if (!newDetectedAt.equals(entity.getDetectedAt())
                        && fallRepo.existsByPatient_PaKeyAndDetectedAt(req.paKey(), newDetectedAt)) {
                    failed++;
                    results.add(new FallEventUpdateResponse.Result(
                            item.fallKey(), "failed",
                            "중복 충돌: (paKey=%d, detectedAt=%s)".formatted(req.paKey(), newDetectedAt)
                    ));
                    continue;
                }

                // 적용
                entity.setSeverity(newSeverity);
                entity.setDetectedAt(newDetectedAt);

                updated++;
                results.add(new FallEventUpdateResponse.Result(
                        item.fallKey(), "updated", "수정 완료"
                ));
            } catch (IllegalArgumentException e) {
                failed++;
                results.add(new FallEventUpdateResponse.Result(
                        item.fallKey(), "failed", e.getMessage()
                ));
            } catch (Exception e) {
                failed++;
                results.add(new FallEventUpdateResponse.Result(
                        item.fallKey(), "failed", "예상치 못한 오류: " + e.getMessage()
                ));
            }
        }

        return new FallEventUpdateResponse(
                req.paKey(), req.updates().size(), updated, skipped, failed, results
        );
    }
}
