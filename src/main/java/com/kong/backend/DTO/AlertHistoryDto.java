package com.kong.backend.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@Schema(description = "알림 히스토리 응답 DTO")
public class AlertHistoryDto {

    @Schema(description = "알림 ID", example = "1")
    private Integer alertId;

    @Schema(description = "이벤트 종류", example = "낙상")
    private String eventType;

    @Schema(description = "알림 레벨", example = "비상")
    private String alertLevel;

    @Schema(description = "감지 시각", example = "2024-04-18T20:07:00")
    private LocalDateTime detectedAt;
}