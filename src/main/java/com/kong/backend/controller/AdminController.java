package com.kong.backend.controller;

import com.kong.backend.DTO.AdminDto;
import com.kong.backend.DTO.AdminLoginRequestDto;
import com.kong.backend.Entity.AdminEntity;
import com.kong.backend.Entity.AdminLoginEntity;
import com.kong.backend.repository.AdminLoginRepository;
import com.kong.backend.repository.AdminRepository;
import com.kong.backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
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
    private final AdminRepository adminRepository;

    @Operation(summary = "관리자 회원가입")
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody AdminDto dto) {
        try {
            adminService.signup(dto);
            return ResponseEntity.ok("관리자 회원가입 성공");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "관리자 로그인")
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody AdminLoginRequestDto dto, HttpSession session) {
        try {
            boolean success = adminService.login(dto.getAdminId(), dto.getAdminPwd());
            if (success) {
                session.setAttribute("admin", dto.getAdminId());
                return ResponseEntity.ok("관리자 로그인 성공");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        return ResponseEntity.badRequest().body("로그인 실패");
    }

    @Operation(summary = "관리자 로그아웃")
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
