package com.kong.backend.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

@Schema(description = "예측 저장 요청 (단건/배치 통합)")
public record PredictionSaveRequest(
        @Schema(description = "환자 PK", example = "123")
        @NotNull Integer paKey,

        @Schema(description = "예측 배열(1~10개)", example = """
        [
          {"diseaseCode":"COLD","riskScore":0.82,"predictedAt":"2025-09-10T05:00:00Z"},
          {"diseaseCode":"ARTH","riskScore":0.64,"predictedAt":"2025-09-10T05:05:00Z"}
        ]""")
        @NotNull @Size(min = 1, max = 10) List<@Valid Item> predictions
) {
    public record Item(
            @Schema(example = "COLD") @NotBlank @Size(max = 32) String diseaseCode,
            @Schema(example = "0.82") @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double riskScore,
            @Schema(example = "2025-09-10T05:00:00Z") @NotNull Instant predictedAt
    ) {}
}
