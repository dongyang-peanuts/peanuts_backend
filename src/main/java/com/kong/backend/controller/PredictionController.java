package com.kong.backend.controller;

import com.kong.backend.DTO.*;
import com.kong.backend.service.PredictionDashboardService;
import com.kong.backend.service.PredictionQueryService;
import com.kong.backend.service.PredictionSaveService;
import com.kong.backend.service.PredictionUpdateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "질병 예측 API", description = "예측 저장/조회 API (단건/배치 저장 통합, 환자별 4개 페이징, 전체 환자 대시보드)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/predictions")
public class PredictionController {

    private final PredictionSaveService saveService;
    private final PredictionQueryService queryService;
    private final PredictionDashboardService dashboardService;
    private final PredictionUpdateService updateService;

    @Operation(summary = "예측 저장",
            responses = {
                    @ApiResponse(responseCode = "201", description = "저장 완료"),
                    @ApiResponse(responseCode = "400", description = "입력 형식 오류"),
                    @ApiResponse(responseCode = "404", description = "존재하지 않는 환자")
            })
    @PostMapping("/save")
    public ResponseEntity<PredictionSaveResponse> save(@RequestBody PredictionSaveRequest request) {
        var result = saveService.save(request);
        return ResponseEntity.status(201).body(result);
    }

    @Operation(summary = "특정 환자 예측 페이징 조회",
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공"),
                    @ApiResponse(responseCode = "404", description = "환자/예측 없음")
            })
    @GetMapping("/patient/{paKey}")
    public ResponseEntity<PageResponse<PredictionDto>> findByPatientPaged(
            @PathVariable Integer paKey,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(required = false) String sort // 기본 predictedAt,desc (서비스에서 처리)
    ) {
        var pageResult = queryService.findByPatientPaged(paKey, page, sort);
        return ResponseEntity.ok(pageResult);
    }

    @Operation(summary = "전체 환자 목록 + 각 환자별 상위 4개",
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공")
            })
    @GetMapping("/patient")
    public ResponseEntity<List<PatientWithPredictionsResponse>> getAllPatientsWithPredictions() {
        var list = dashboardService.getAllPatientsWithLatestPredictions();
        // 필요 시 N+1 대안:
        // var list = dashboardService.getAllPatientsWithLatestPredictions_NPlus1();
        return ResponseEntity.ok(list);
    }

    // ✅ 여러 항목 동시 부분 수정 (이전 이름 사용, Batch 미사용)
    @Operation(summary = "예측 수정",
            responses = {
                    @ApiResponse(responseCode = "200", description = "수정 결과 반환"),
                    @ApiResponse(responseCode = "400", description = "입력 형식 오류"),
                    @ApiResponse(responseCode = "409", description = "중복 충돌 존재(항목별 failed)")
            })
    @PatchMapping("/patient/{paKey}")
    public ResponseEntity<PredictionUpdateResponse> updateMany(
            @PathVariable Integer paKey,
            @RequestBody PredictionUpdateRequest request
    ) {
        if (!paKey.equals(request.paKey())) {
            return ResponseEntity.badRequest().build();
        }
        var result = updateService.update(request);
        return ResponseEntity.ok(result);
    }
}
