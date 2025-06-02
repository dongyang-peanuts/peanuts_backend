package com.kong.backend.service;

import com.kong.backend.DTO.PatientDto;
import com.kong.backend.DTO.PatientInfoDto;
import com.kong.backend.DTO.UserDto;
import com.kong.backend.Entity.LoginEntity;
import com.kong.backend.Entity.PatientEntity;
import com.kong.backend.Entity.PatientInfoEntity;
import com.kong.backend.Entity.UserEntity;
import com.kong.backend.exception.PasswordMismatchException;
import com.kong.backend.exception.UserNotFoundException;
import com.kong.backend.repository.LoginRepository;
import com.kong.backend.repository.PatientInfoRepository;
import com.kong.backend.repository.PatientRepository;
import com.kong.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final LoginRepository loginRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final PatientRepository patientRepository;
    private final PatientInfoRepository patientInfoRepository;


    public UserEntity signup(UserDto dto) {
        // 이메일 중복 체크
        if (userRepository.findByUserEmail(dto.getUserEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        // 사용자 저장
        UserEntity user = new UserEntity();
        user.setUserEmail(dto.getUserEmail());
        user.setUserPwd(passwordEncoder.encode(dto.getUserPwd()));
        user.setUserAddr(dto.getUserAddr());
        user.setUserNumber(dto.getUserNumber());
        user.setProNum(dto.getProNum());
        user.setAuth(0); // 일반 사용자
        user = userRepository.save(user);

        // 환자 저장
        List<PatientEntity> patientEntities = new ArrayList<>();
        for (PatientDto patientDto : dto.getPatients()) {
            PatientEntity patient = new PatientEntity();
            patient.setPaName(patientDto.getPaName());
            patient.setPaAddr(patientDto.getPaAddr());
            patient.setPaAge(patientDto.getPaAge());
            patient.setPaHei(patientDto.getPaHei());
            patient.setPaWei(patientDto.getPaWei());
            patient.setUser(user); // 연관관계 설정

            patient = patientRepository.save(patient);

// 환자 정보 저장
            List<PatientInfoEntity> infoEntities = new ArrayList<>();
            for (PatientInfoDto infoDto : patientDto.getInfos()) {
                PatientInfoEntity info = new PatientInfoEntity();
                info.setPatient(patient);
                info.setPaFact(infoDto.getPaFact());
                info.setPaPrct(infoDto.getPaPrct());
                info.setPaDi(infoDto.getPaDi());
                info.setPaDise(infoDto.getPaDise());
                info.setPaExti(infoDto.getPaExti());
                info.setPaBest(infoDto.getPaBest());
                info.setPaMedi(infoDto.getPaMedi());
                infoEntities.add(info);
            }
            patientInfoRepository.saveAll(infoEntities);

            patient.setInfos(infoEntities);
            patientEntities.add(patient);
        }

        user.setPatients(patientEntities);
        return user;
    }

    public boolean login(String email, String rawPwd) {
        UserEntity user = userRepository.findByUserEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자 이메일이 존재하지 않습니다."));

        if (!passwordEncoder.matches(rawPwd, user.getUserPwd())) {
            throw new PasswordMismatchException("비밀번호가 틀렸습니다.");
        }

        LoginEntity loginRecord = LoginEntity.builder()
                .userEmail(email)
                .loginTime(LocalDateTime.now())
                .Auth(0)
                .build();

        loginRepository.save(loginRecord);
        return true;
    }
}