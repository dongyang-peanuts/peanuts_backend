package com.kong.backend.service;

import com.kong.backend.DTO.AdminDto;
import com.kong.backend.Entity.AdminEntity;
import com.kong.backend.Entity.AdminLoginEntity;
import com.kong.backend.exception.AdminNotFoundException;
import com.kong.backend.exception.PasswordMismatchException;
import com.kong.backend.repository.AdminLoginRepository;
import com.kong.backend.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final AdminLoginRepository adminLoginRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * 로그인 시도 - 성공 시 로그인 로그 저장
     * @param adminId 로그인 ID
     * @param rawPwd 입력한 비밀번호
     * @return 로그인 성공 여부 (항상 true, 실패 시 예외 발생)
     */
    public boolean login(String adminId, String rawPwd) {
        AdminEntity admin = adminRepository.findByAdminId(adminId)
                .orElseThrow(() -> new AdminNotFoundException("관리자 ID가 존재하지 않습니다."));

        if (!passwordEncoder.matches(rawPwd, admin.getAdminPwd())) {
            throw new PasswordMismatchException("비밀번호가 틀렸습니다.");
        }

        // 로그인 성공 → 로그인 기록 저장
        adminLoginRepository.save(AdminLoginEntity.builder()
                .adminId(adminId)
                .adminloginTime(LocalDateTime.now())
                .Auth(1)
                .build());

        return true;
    }

    /**
     * 관리자 회원가입
     * @param dto 관리자 정보
     */
    public void signup(AdminDto dto) {
        if (adminRepository.findByAdminId(dto.getAdminId()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 관리자 ID입니다.");
        }

        AdminEntity admin = AdminEntity.builder()
                .adminId(dto.getAdminId())
                .adminPwd(passwordEncoder.encode(dto.getAdminPwd()))
                .Auth(1)
                .build();

        adminRepository.save(admin);
    }
}
