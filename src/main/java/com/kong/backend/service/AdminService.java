package com.kong.backend.service;

import com.kong.backend.Entity.AdminEntity;
import com.kong.backend.Entity.LoginEntity;
import com.kong.backend.repository.AdminRepository;
import com.kong.backend.repository.LoginRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final LoginRepository loginRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public boolean login(String adminId, String rawPwd) {
        AdminEntity admin = adminRepository.findByAdminId(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자 ID가 존재하지 않습니다."));

        if (!passwordEncoder.matches(rawPwd, admin.getAdminPwd())) {
            throw new IllegalArgumentException("비밀번호가 틀렸습니다.");
        }

        // 로그인 성공 시 login 테이블에 기록
        LoginEntity login = LoginEntity.builder()
                .userEmail(adminId)
                .loginTime(LocalDateTime.now())
                .Auth(1)
                .build();

        loginRepository.save(login);

        return true;
    }
}
