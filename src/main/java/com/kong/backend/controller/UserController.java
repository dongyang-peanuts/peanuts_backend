package com.kong.backend.controller;

import com.kong.backend.DTO.*;
import com.kong.backend.Entity.LoginEntity;
import com.kong.backend.repository.LoginRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.kong.backend.service.UserService;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.Optional;

@Tag(name = "User API", description = "회원가입, 로그인, 로그아웃 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final LoginRepository loginRepository;

    // ✅ 사용자 회원가입
    @Operation(summary = "사용자 회원가입", responses = {
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "중복된 이메일")
    })
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody UserSignUpDto dto) {
        userService.signup(dto);
        return ResponseEntity.ok("회원가입 성공");
    }

    // ✅ 사용자 로그인
    @Operation(summary = "사용자 로그인", responses = {
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

    // ✅ 사용자 로그아웃
    @Operation(summary = "사용자 로그아웃", responses = {
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

    // ✅ 사용자 주소수정
    @Operation( summary = "사용자 주소 수정", responses = {
            @ApiResponse(responseCode = "200", description = "주소 수정 성공"),
            @ApiResponse(responseCode = "404", description = "해당 유저 없음")
    })
    @PatchMapping("/address/{userKey}")
    public ResponseEntity<String> updateAddress(
            @PathVariable Integer userKey,
            @RequestBody String newAddress) {

        userService.updateUserAddress(userKey, newAddress);
        return ResponseEntity.ok("주소 수정 성공");
    }

    // ✅ 환자 정보 조회
    @Operation( summary = "사용자의 환자 정보 조회", responses = {
            @ApiResponse(responseCode = "200", description = "환자 조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 환자 없음")
    })
    @GetMapping("/{userKey}/patients")
    public ResponseEntity<List<PatientDto>> getPatientsWithInfo(@PathVariable Integer userKey) {
        List<PatientDto> patients = userService.getPatientsWithInfoByUserKey(userKey);
        return ResponseEntity.ok(patients);
    }

    // ✅ 환자 정보 수정
    @Operation(summary = "사용자의 환자 및 환자정보 수정", responses = {
                @ApiResponse(responseCode = "200", description = "환자 정보 수정 완료"),
                @ApiResponse(responseCode = "404", description = "해당 유저 없음"),
                @ApiResponse(responseCode = "400", description = "요청 형식 오류 또는 유효성 실패"),
                @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PutMapping("/{userKey}/patients")
    public ResponseEntity<String> updatePatients(
            @PathVariable Integer userKey,
            @RequestBody List<PatientDto> patients) {

        if (patients == null || patients.isEmpty()) {
            return ResponseEntity.badRequest().body("환자 정보가 비어 있습니다.");
        }

        userService.updatePatientInfo(userKey, patients);
        return ResponseEntity.ok("환자 정보 수정 완료");
    }

    // ✅ 사용자 비밀번호 수정
    @Operation(summary = "사용자 비밀번호 수정", responses = {
            @ApiResponse(responseCode = "200", description = "비밀번호 수정 성공"),
            @ApiResponse(responseCode = "400", description = "기존 비밀번호 불일치"),
            @ApiResponse(responseCode = "404", description = "해당 유저 없음")
    })
    @PatchMapping("/{userKey}/password")
    public ResponseEntity<String> updatePassword(
            @PathVariable Integer userKey,
            @RequestBody PasswordUpdateDto dto) {

        userService.updateUserPassword(userKey, dto);
        return ResponseEntity.ok("비밀번호 수정 성공");
    }

    @Operation(summary = "사용자 전화번호 수정", responses = {
            @ApiResponse(responseCode = "200", description = "전화번호 수정 성공"),
            @ApiResponse(responseCode = "404", description = "해당 유저 없음")
    })
    @PatchMapping("/{userKey}/number")
    public ResponseEntity<String> updateUserNumber(
            @PathVariable Integer userKey,
            @RequestBody UserNumberUpdateDto dto) {

        userService.updateUserNumber(userKey, dto);
        return ResponseEntity.ok("전화번호 수정 성공");
    }

}