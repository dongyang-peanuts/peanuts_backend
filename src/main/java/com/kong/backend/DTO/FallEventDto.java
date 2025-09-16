package com.kong.backend.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "낙상 이벤트 DTO")
public record FallEventDto(
        @Schema(example = "45") Integer fallKey,
        @Schema(example = "123") Integer paKey,
        @Schema(example = "HIGH") String severity,
        @Schema(example = "2025-09-10T07:00:00Z") Instant detectedAt,
        @Schema(example = "2025-09-10T07:00:01Z") Instant createdAt
) {}
