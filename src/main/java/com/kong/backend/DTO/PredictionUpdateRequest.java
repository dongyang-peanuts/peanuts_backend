package com.kong.backend.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public record PredictionUpdateRequest(
        @Schema(example = "123") @NotNull Integer paKey,
        @NotNull @Size(min = 1, max = 50) List<@Valid Item> updates
) {
    public record Item(
            @Schema(example = "16") @NotNull Integer predKey,        // 수정 대상
            @Schema(example = "COLD") String diseaseCode,            // null이면 미수정
            @Schema(example = "0.91") Double riskScore,              // null이면 미수정
            @Schema(example = "2025-09-10T07:00:00Z") Instant predictedAt // null이면 미수정
    ) {}
}
