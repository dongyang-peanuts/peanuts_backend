package com.kong.backend.DTO;

import lombok.*;


@Data
@AllArgsConstructor
public class UserLoginResponseDto {
    private Integer userKey;
    private String userEmail;
}