package com.kong.backend.controller;

import com.kong.backend.DTO.AlertHistoryDto;
import com.kong.backend.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "관리자 알림 API", description = "관리자용 사용자 알림 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/users/alerts")
public class AdminAlertController {

    private final AlertService alertService;

    // ✅ (1) 관리자 - 특정 사용자 알림 조회
    @Operation(summary = "특정 사용자의 알림 히스토리 조회", responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공"),
                    @ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    @GetMapping("/{userKey}")
    public ResponseEntity<List<AlertHistoryDto>> getAlertsByUser(@PathVariable Integer userKey) {
        List<AlertHistoryDto> history = alertService.getAlertHistoryByUserKey(userKey);
        return ResponseEntity.ok(history);
    }

    // ✅ (2) 관리자 - 전체 알림 조회
    @Operation(summary = "모든 사용자 알림 전체 조회", responses = {
                    @ApiResponse(responseCode = "200", description = "전체 알림 조회 성공")
    })
    @GetMapping
    public ResponseEntity<List<AlertHistoryDto>> getAllAlerts() {
        List<AlertHistoryDto> allAlerts = alertService.getAllAlertHistories();
        return ResponseEntity.ok(allAlerts);
    }
}
