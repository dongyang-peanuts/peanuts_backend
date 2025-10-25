package com.kong.backend.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Schema(description = "알림 저장 요청 바디 DTO")
public record SaveAlertRequest(

        @NotBlank
        @Schema(description = "이벤트 타입", example = "FALL_DETECTED")
        String eventType,

        @Schema(description = "감지 시각(선택). 미지정 시 ts 또는 서버시간 사용",
                example = "2025-10-23T03:12:45")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime detectedAt,

        @NotNull
        @Schema(description = "유저 키", example = "1")
        Integer userKey,

        @Schema(description = "낙상 확률(0~1)", example = "0.87")
        Double layRate,

        @Schema(description = "배회 확률(0~1)", example = "0.42")
        Double prob,

        @Schema(description = "값", example = "1734949883.125")
        Double ts,

        @Schema(description = "동영상 ID", example = "123")
        Integer videoId
) {}
