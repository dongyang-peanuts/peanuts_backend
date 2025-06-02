package com.kong.backend.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
@Schema(description = "환자 등록 DTO")
public class PatientDto {

//    @Schema(description = "소셜 고유번호")
//    private Integer sociKey;

    @Schema(description = "환자 이름", example = "환자이름")
    private String paName;

    @Schema(description = "주소", example = "환자주소")
    private String paAddr;

    @Schema(description = "나이", example = "환자나이")
    private Integer paAge;

    @Schema(description = "키", example = "환자 키")
    private Double paHei;

    @Schema(description = "체중",example = "환자 체중")
    private Double paWei;

    @Schema(description = "환자의 건강 정보 목록")
    private List<PatientInfoDto> infos;
}
