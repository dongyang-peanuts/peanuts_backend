package com.kong.backend.controller;

import com.kong.backend.DTO.AdminDto;
import com.kong.backend.DTO.AdminLoginRequestDto;
import com.kong.backend.Entity.AdminLoginEntity;
import com.kong.backend.repository.AdminLoginRepository;
import com.kong.backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Tag(name = "관리자 API", description = "회원정보관리, 모니터링 API")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AdminLoginRepository adminLoginRepository;

    // ✅ 관리자 회원가입
    @Operation(summary = "관리자 회원가입", responses = {
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "중복된 ID")
    })
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody AdminDto dto) {
        adminService.signup(dto);
        return ResponseEntity.ok("관리자 회원가입 성공");
    }

    // ✅ 관리자 로그인
    @Operation(summary = "관리자 로그인", responses = {
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "비밀번호 틀림"),
            @ApiResponse(responseCode = "404", description = "ID 없음")
    })
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody AdminLoginRequestDto dto, HttpSession session) {
        adminService.login(dto.getAdminId(), dto.getAdminPwd());
        session.setAttribute("admin", dto.getAdminId());
        return ResponseEntity.ok("관리자 로그인 성공");
    }

    // ✅ 관리자 로그아웃
    @Operation(summary = "관리자 로그아웃", responses = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "404", description = "로그인 기록 없음")
    })
    @PostMapping("/logout/{adminId}")
    public ResponseEntity<String> logout(@PathVariable String adminId) {
        Optional<AdminLoginEntity> login = adminLoginRepository.findTopByAdminIdOrderByAdminloginTimeDesc(adminId);

        if (login.isEmpty()) {
            return ResponseEntity.status(404).body("로그인 기록 없음");
        }

        adminLoginRepository.delete(login.get());
        return ResponseEntity.ok("관리자 로그아웃 성공");
    }
}
