package com.kong.backend.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class UserDto {


    @Schema(description = "이메일", example = "test@example.com")
    private String userEmail;

    @Schema(description = "비밀번호", example = "1234")
    private String userPwd;

    @Schema(description = "주소", example = "서울시 마포구")
    private String userAddr;

    @Schema(description = "전화번호", example = "01012345678")
    private String userNumber;

    @Schema(description = "기기번호 번호", example = "PROMO01")
    private String proNum;

    @Schema(description = "등록할 환자 목록")
    private List<PatientDto> patients;
}