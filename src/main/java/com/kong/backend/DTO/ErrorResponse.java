package com.kong.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 예외 발생 시 JSON 형태로 반환될 응답 구조
 */
@Getter
@AllArgsConstructor
public class ErrorResponse {
    private int status;     // HTTP 상태 코드
    private String message; // 에러 메시지
}
