package com.kong.backend.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class LoginRequestDto {

    @Schema(description = "이메일", example = "test@example.com")
    private String userEmail;

    @Schema(description = "비밀번호", example = "1234")
    private String userPwd;
}