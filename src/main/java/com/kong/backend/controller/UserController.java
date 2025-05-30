package com.kong.backend.controller;

import com.kong.backend.DTO.LoginRequestDto;
import com.kong.backend.Entity.LoginEntity;
import com.kong.backend.repository.LoginRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    // ✅ 유저 회원가입
    @Operation(summary = "회원가입", responses = {
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "중복된 이메일")
    })
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody UserDto dto) {
        userService.signup(dto);
        return ResponseEntity.ok("회원가입 성공");
    }

    // ✅ 유저 로그인
    @Operation(summary = "로그인", responses = {
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "비밀번호 틀림"),
            @ApiResponse(responseCode = "404", description = "이메일 없음")
    })
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequestDto dto, HttpSession session) {
        userService.login(dto.getUserEmail(), dto.getUserPwd());
        session.setAttribute("user", dto.getUserEmail());
        return ResponseEntity.ok("로그인 성공");
    }

    // ✅ 유저 로그아웃
    @Operation(summary = "로그아웃", responses = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "404", description = "로그인 기록 없음")
    })
    @PostMapping("/logout/{userEmail}")
    public ResponseEntity<String> logout(@PathVariable String userEmail) {
        Optional<LoginEntity> login = loginRepository.findTopByUserEmailOrderByLoginTimeDesc(userEmail);

        if (login.isEmpty()) {
            return ResponseEntity.status(404).body("로그인 기록 없음");
        }

        loginRepository.delete(login.get());
        return ResponseEntity.ok("로그아웃 성공");
    }
}