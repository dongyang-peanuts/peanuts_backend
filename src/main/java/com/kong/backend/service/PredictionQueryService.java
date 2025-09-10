package com.kong.backend.service;

import com.kong.backend.DTO.PageResponse;
import com.kong.backend.DTO.PredictionDto;
import com.kong.backend.Entity.PredictionEntity;
import com.kong.backend.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PredictionQueryService {

    private final PredictionRepository repo;

    // ✅ 환자별 페이징 조회 (항상 4개씩)
    public PageResponse<PredictionDto> findByPatientPaged(Integer paKey, Integer page, String sort) {
        int pageNum = (page == null || page < 0) ? 0 : page;
        int pageSize = 4; // 고정 4개

        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(pageNum, pageSize, sortObj);

        Specification<PredictionEntity> spec = (root, q, cb) ->
                cb.equal(root.get("patient").get("paKey"), paKey);

        Page<PredictionEntity> data = repo.findAll(spec, pageable);
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
        if (sort == null || sort.isBlank())
            return Sort.by(Sort.Direction.DESC, "predictedAt");
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        Sort.Direction dir = (parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, field);
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
