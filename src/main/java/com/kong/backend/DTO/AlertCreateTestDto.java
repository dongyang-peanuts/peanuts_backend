package com.kong.backend.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Schema(description = "알림 테스트 생성 요청 DTO")
public class AlertCreateTestDto {

    @Schema(description = "사용자 고유키", example = "1")
    private Integer userKey;

    @Schema(description = "이벤트 종류", example = "낙상, 배회, 손떨림")
    private String eventType;

    @Schema(description = "알림 수준", example = "비상, 주의, 경고") // 주의, 경고, 비상
    private String alertLevel;
}
