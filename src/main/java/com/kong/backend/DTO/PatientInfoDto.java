package com.kong.backend.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Schema(description = "환자 정보 DTO")
public class PatientInfoDto {

    @Schema(description = "낙상 횟수" ,example = "낙상 횟수")
    private Integer paFact;

    @Schema(description = "욕창 횟수", example = "욕창 횟수")
    private Integer paPrct;

    @Schema(description = "질병명", example = "질병명")
    private String paDi;

    @Schema(description = "질병 중증도", example = "질병중증도")
    private String paDise;

    @Schema(description = "운동 시간", example = "운동시간")
    private String paExti;

    @Schema(description = "거동 상태", example = "거동 상태")
    private String paBest;

    @Schema(description = "복용 약", example = "복용 약")
    private String paMedi;
}
