package com.kong.backend.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

@Schema(description = "낙상 여러 건 부분 수정 요청 (같은 paKey 소속만 허용)")
public record FallEventUpdateRequest(
        @Schema(example = "123") @NotNull Integer paKey,
        @NotNull @Size(min = 1, max = 50) List<@Valid Item> updates
) {
    public record Item(
            @Schema(example = "45") @NotNull Integer fallKey,
            @Schema(example = "HIGH") String severity,            // null이면 미수정
            @Schema(example = "2025-09-10T08:00:00Z") Instant detectedAt // null이면 미수정
    ) {}
}
