package com.kong.backend.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AdminDto {

    @Schema(description = "관리자 아이디", example = "admin")
    private String adminId;

    @Schema(description = "관리자 비밀번호", example = "1234")
    private String adminPwd;

}
