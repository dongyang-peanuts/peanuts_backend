package com.kong.backend.controller;

import com.kong.backend.DTO.LoginRequestDto;
import com.kong.backend.Entity.LoginEntity;
import com.kong.backend.repository.LoginRepository;
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
    private final LoginRepository loginRepository;

    @Operation(summary = "관리자 로그인")
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequestDto dto, HttpSession session) {
        try {
            boolean success = adminService.login(dto.getUserEmail(), dto.getUserPwd());
            if (success) {
                session.setAttribute("admin", dto.getUserEmail());
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
        Optional<LoginEntity> login = loginRepository.findTopByUserEmailOrderByLoginTimeDesc(adminId);

        if (login.isEmpty()) {
            return ResponseEntity.status(404).body("로그인 기록 없음");
        }

        loginRepository.delete(login.get());
        return ResponseEntity.ok("관리자 로그아웃 성공");
    }
}
