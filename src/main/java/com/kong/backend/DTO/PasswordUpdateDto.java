package com.kong.backend.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PasswordUpdateDto {
    private String currentPwd;   // 기존 비밀번호
    private String newPwd;       // 새 비밀번호
}
