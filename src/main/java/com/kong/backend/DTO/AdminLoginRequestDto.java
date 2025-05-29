package com.kong.backend.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AdminLoginRequestDto {

    @Schema(description = "괸리자 아이디", example = "admin")
    private String adminId;

    @Schema(description = "관리자 비밀번호", example = "****")
    private String adminPwd;

}
