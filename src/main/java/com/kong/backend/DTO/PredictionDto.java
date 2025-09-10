package com.kong.backend.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record PredictionDto(
        @Schema(example = "21") Integer predKey,
        @Schema(example = "123") Integer paKey,
        @Schema(example = "COLD") String diseaseCode,
        @Schema(example = "0.82") Double riskScore,
        @Schema(example = "2025-09-10T06:00:00Z") Instant predictedAt,
        @Schema(example = "2025-09-10T06:00:01Z") Instant createdAt
) {}
