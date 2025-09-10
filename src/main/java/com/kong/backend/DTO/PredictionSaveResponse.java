package com.kong.backend.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record PredictionSaveResponse(
        @Schema(example = "123") Integer paKey,
        @Schema(example = "2") int total,
        @Schema(example = "2") int created,
        @Schema(example = "0") int skipped,
        List<ItemResult> results
) {
    public record ItemResult(
            @Schema(example = "16") Integer predKey,
            @Schema(example = "COLD") String diseaseCode,
            @Schema(example = "created") String status, // created | skipped | error
            @Schema(example = "저장됨") String message
    ) {}
}
