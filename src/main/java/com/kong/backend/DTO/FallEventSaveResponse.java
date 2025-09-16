package com.kong.backend.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "낙상 이벤트 저장 응답")
public record FallEventSaveResponse(
        @Schema(description = "저장된 낙상 PK", example = "45") Integer fallKey,
        @Schema(description = "환자 PK", example = "123") Integer paKey,
        @Schema(description = "낙상 심각도", example = "HIGH") String severity,
        @Schema(description = "낙상 감지 시각(UTC)", example = "2025-09-10T07:00:00Z") Instant detectedAt
) {}
