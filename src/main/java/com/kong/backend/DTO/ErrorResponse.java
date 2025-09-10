package com.kong.backend.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "에러 응답")
public record ErrorResponse(
        @Schema(example = "409") int status,
        @Schema(example = "중복 충돌: (paKey=123, diseaseCode=ARTH, predictedAt=2025-09-10T05:05:00Z)")
        String message
) {}
