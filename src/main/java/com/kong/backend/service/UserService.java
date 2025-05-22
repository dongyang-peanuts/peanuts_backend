package com.kong.backend.service;

import com.kong.backend.DTO.UserDto;
import com.kong.backend.Entity.LoginEntity;
import com.kong.backend.Entity.UserEntity;
import com.kong.backend.repository.LoginRepository;
import com.kong.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final LoginRepository loginRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserEntity signup(UserDto dto) {

        // 이메일 중복 검사
        if (userRepository.findByUserEmail(dto.getUserEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        UserEntity user = new UserEntity();
        user.setUserEmail(dto.getUserEmail());
        user.setUserPwd(passwordEncoder.encode(dto.getUserPwd()));
        user.setUserAddr(dto.getUserAddr());
        user.setUserNumber(dto.getUserNumber());
        user.setProNum(dto.getProNum());

        return userRepository.save(user);
    }

    public boolean login(String email, String rawPwd) {
        Optional<UserEntity> userOpt = userRepository.findByUserEmail(email);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("이메일이 존재하지 않습니다.");
        }

        UserEntity user = userOpt.get();
        if (!passwordEncoder.matches(rawPwd, user.getUserPwd())) {
            throw new IllegalArgumentException("비밀번호가 틀렸습니다.");
        }
        if (userOpt.isEmpty()) return false;
        LoginEntity loginRecord = LoginEntity.builder()
                .userEmail(email)
                .loginTime(LocalDateTime.now())
                .Auth(0) // 일반 사용자
                .build();

        loginRepository.save(loginRecord);

        return true;
    }
}