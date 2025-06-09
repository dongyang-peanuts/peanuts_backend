package com.kong.backend.controller;

import com.kong.backend.DTO.AdminDto;
import com.kong.backend.DTO.AdminLoginRequestDto;
import com.kong.backend.DTO.UserDto;
import com.kong.backend.Entity.AdminLoginEntity;
import com.kong.backend.Entity.UserEntity;
import com.kong.backend.repository.AdminLoginRepository;
import com.kong.backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    // ✅ 사용자 정보조회
    @Operation(summary = "전체 회원 목록 조회", description = "모든 사용자 정보를 조회")
    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = adminService.getAllUsersWithDetails();
        return ResponseEntity.ok(users);
    }

    // ✅ 사용자정보 상세조회
    @Operation(summary = "회원 상세 조회", description = "userKey로 회원 + 환자 + 환자정보 전체 조회")
    @GetMapping("/users/{userKey}")
    public ResponseEntity<UserDto> getUserDetails(@PathVariable Integer userKey) {
        UserDto userDto = adminService.getUserDetails(userKey);
        return ResponseEntity.ok(userDto);
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

    // ✅ 사용자 정보 삭제
    @Operation(summary = "사용자 삭제", responses = {
                    @ApiResponse(responseCode = "200", description = "삭제 성공"),
                    @ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    @DeleteMapping("/users/{userKey}")
    public ResponseEntity<String> deleteUser(@PathVariable Integer userKey) {
        adminService.deleteUserByKey(userKey);
        return ResponseEntity.ok("사용자 삭제 완료");
    }
}
