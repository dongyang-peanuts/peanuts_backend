package com.kong.backend.controller;

import com.kong.backend.DTO.LoginRequestDto;
import com.kong.backend.Entity.LoginEntity;
import com.kong.backend.repository.LoginRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.kong.backend.DTO.UserDto;
import com.kong.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

import java.util.Optional;

@Tag(name = "User API", description = "회원가입, 로그인, 로그아웃 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final LoginRepository loginRepository;

    @Operation(summary = "회원가입", description = "이메일 중복 체크 포함")
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody UserDto dto) {
        try {
            userService.signup(dto);
            return ResponseEntity.ok("회원가입 성공");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "로그인", description = "비밀번호 검증 포함")
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequestDto dto, HttpSession session) {
        try {
            boolean success = userService.login(dto.getUserEmail(), dto.getUserPwd());
            if (success) {
                session.setAttribute("user", dto.getUserEmail());
                return ResponseEntity.ok("로그인 성공");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        return ResponseEntity.badRequest().body("로그인 실패");
    }

    @Operation(summary = "로그아웃", description = "세션 종료 처리")
    @PostMapping("/logout/{userEmail}")
    public ResponseEntity<String> logout(@PathVariable String userEmail) {

        // login 테이블에서 최근 로그인 기록 조회
        Optional<LoginEntity> login = loginRepository.findTopByUserEmailOrderByLoginTimeDesc(userEmail);

        if (login.isEmpty()) {
            return ResponseEntity.status(404).body("로그인 기록 없음");
        }

        loginRepository.delete(login.get());
        return ResponseEntity.ok("로그아웃 성공");
    }
}