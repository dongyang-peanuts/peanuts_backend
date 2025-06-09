package com.kong.backend.controller;

import com.kong.backend.DTO.AlertCreateTestDto;
import com.kong.backend.DTO.AlertHistoryDto;
import com.kong.backend.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "사용자 알림 API", description = "위험 감지 알림 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/user/alerts")
public class AlertController {

    private final AlertService alertService;

    @Operation(summary = "사용자 감지 히스토리 조회", responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공"),
                    @ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    @GetMapping("/{userKey}")
    public ResponseEntity<List<AlertHistoryDto>> getAlertHistory(@PathVariable Integer userKey) {
        List<AlertHistoryDto> history = alertService.getAlertHistoryByUserKey(userKey);
        return ResponseEntity.ok(history);
    }

    @Operation(summary = "알림 생성 (테스트용)", responses = {
                    @ApiResponse(responseCode = "200", description = "모든 알림 저장 성공"),
                    @ApiResponse(responseCode = "400", description = "입력 형식 오류"),
                    @ApiResponse(responseCode = "404", description = "존재하지 않는 유저 포함")
    })
    @PostMapping("/test")
    public ResponseEntity<String> createMultipleTestAlerts(@RequestBody List<AlertCreateTestDto> alertList) {
        alertService.saveMultipleTestAlerts(alertList);
        return ResponseEntity.ok("테스트 알림 여러 개 저장 완료");
    }
}
