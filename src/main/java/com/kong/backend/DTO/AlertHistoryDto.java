package com.kong.backend.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "알림 히스토리 응답 DTO")
public class AlertHistoryDto {

    @Schema(description = "알림 ID", example = "1")
    private Integer alertId;

    @Schema(description = "이벤트 종류", example = "낙상")
    private String eventType;

//    @Schema(description = "알림 레벨", example = "HIGH")
//    private String alertLevel;

    @Schema(description = "감지 시각", example = "2025-09-15T22:10:00")
    private LocalDateTime detectedAt;

    /* ---- 낙상모델 확장 필드 ---- */

//    @Schema(description = "자세 상태", example = "lay")
//    private String pose;   // stand | sit | lay | unknown

    @Schema(description = "누움 비율", example = "0.85")
    private Double layRate;

    @Schema(description = "배회 확률", example = "0.65")
    private Double prob;

//    @Schema(description = "낙상 여부", example = "true")
//    private Boolean fall;

    @Schema(description = "원본 epoch timestamp", example = "1757920000.1234")
    private Double ts;
}
