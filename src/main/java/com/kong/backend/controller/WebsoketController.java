package com.kong.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Websoket API", description = "카메라 모듈 연동")
public class WebsoketController {
    @GetMapping("/ping")
    @Operation(summary = "Ping 테스트", description = "서버 응답 확인용 API")
    public String ping() {
        return "pong";
    }
}
