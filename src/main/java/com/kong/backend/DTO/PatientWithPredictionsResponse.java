package com.kong.backend.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record PatientWithPredictionsResponse(
        @Schema(example = "123") Integer paKey,
        @Schema(example = "홍길동") String paName,
        @Schema(example = "서울시 강남구") String paAddr,
        @Schema(example = "78") Integer paAge,
        @Schema(example = "168.5") Double paHei,
        @Schema(example = "62.3") Double paWei,
        List<PredictionDto> latestPredictions // 항상 최대 4건
) {}
