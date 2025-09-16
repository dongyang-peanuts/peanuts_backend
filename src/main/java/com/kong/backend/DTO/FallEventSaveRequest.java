package com.kong.backend.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "낙상 이벤트 저장 요청")
public record FallEventSaveRequest(
        @Schema(description = "환자 PK", example = "123") Integer paKey,
        @Schema(description = "낙상 심각도 (LOW/MEDIUM/HIGH/CRITICAL)", example = "HIGH") String severity,
        @Schema(description = "낙상 감지 시각(UTC)", example = "2025-09-10T07:00:00Z") Instant detectedAt
) {}
