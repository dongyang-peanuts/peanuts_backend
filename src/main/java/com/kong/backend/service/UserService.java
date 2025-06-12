package com.kong.backend.service;

import com.kong.backend.DTO.*;
import com.kong.backend.Entity.*;
import com.kong.backend.exception.PasswordMismatchException;
import com.kong.backend.exception.UserNotFoundException;
import com.kong.backend.repository.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final AlertHistoryRepository alertHistoryRepository;


    public UserEntity signup(UserSignUpDto dto) {
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
        user.setAuth(0); // 일반 사용자
        user.setSignupDate(LocalDate.now());
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

    public UserEntity login(String email, String rawPwd) {
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
        return user;
    }

    public void updateUserAddress(Integer userKey, String newAddress) {
        UserEntity user = userRepository.findById(userKey)
                .orElseThrow(() -> new UserNotFoundException("해당 유저가 존재하지 않습니다."));
        user.setUserAddr(newAddress);
        userRepository.save(user);
    }

    public List<PatientDto> getPatientsWithInfoByUserKey(Integer userKey) {
        UserEntity user = userRepository.findById(userKey)
                .orElseThrow(() -> new UserNotFoundException("해당 유저가 존재하지 않습니다."));

        List<PatientDto> patientDtoList = new ArrayList<>();
        for (PatientEntity patient : user.getPatients()) {
            PatientDto dto = new PatientDto();
            dto.setPaName(patient.getPaName());
            dto.setPaAddr(patient.getPaAddr());
            dto.setPaAge(patient.getPaAge());
            dto.setPaHei(patient.getPaHei());
            dto.setPaWei(patient.getPaWei());

            List<PatientInfoDto> infoDtos = new ArrayList<>();
            for (PatientInfoEntity info : patient.getInfos()) {
                PatientInfoDto infoDto = new PatientInfoDto();
                infoDto.setPaFact(info.getPaFact());
                infoDto.setPaPrct(info.getPaPrct());
                infoDto.setPaDi(info.getPaDi());
                infoDto.setPaDise(info.getPaDise());
                infoDto.setPaExti(info.getPaExti());
                infoDto.setPaBest(info.getPaBest());
                infoDto.setPaMedi(info.getPaMedi());
                infoDtos.add(infoDto);
            }

            dto.setInfos(infoDtos);
            patientDtoList.add(dto);
        }

        return patientDtoList;
    }

    @Transactional
    public void updatePatientInfo(Integer userKey, List<PatientDto> newPatientList) {
        UserEntity user = userRepository.findById(userKey)
                .orElseThrow(() -> new UserNotFoundException("해당 유저가 존재하지 않습니다."));

        // 기존 환자 정보 삭제
        List<PatientEntity> existingPatients = user.getPatients();
        existingPatients.clear();

        for (PatientDto patientDto : newPatientList) {
            PatientEntity patient = new PatientEntity();
            patient.setPaName(patientDto.getPaName());
            patient.setPaAddr(patientDto.getPaAddr());
            patient.setPaAge(patientDto.getPaAge());
            patient.setPaHei(patientDto.getPaHei());
            patient.setPaWei(patientDto.getPaWei());
            patient.setUser(user);

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

            patient.setInfos(infoEntities);
            existingPatients.add(patient); // 기존 컬렉션에 add
        }

        userRepository.save(user);
    }

    public void updateUserPassword(Integer userKey, PasswordUpdateDto dto) {
        UserEntity user = userRepository.findById(userKey)
                .orElseThrow(() -> new UserNotFoundException("해당 유저가 존재하지 않습니다."));

        if (!passwordEncoder.matches(dto.getCurrentPwd(), user.getUserPwd())) {
            throw new PasswordMismatchException("기존 비밀번호가 일치하지 않습니다.");
        }

        user.setUserPwd(passwordEncoder.encode(dto.getNewPwd()));
        userRepository.save(user);
    }

    public void updateUserNumber(Integer userKey, UserNumberUpdateDto dto) {
        UserEntity user = userRepository.findById(userKey)
                .orElseThrow(() -> new UserNotFoundException("해당 유저가 존재하지 않습니다."));

        user.setUserNumber(dto.getNewUserNumber());
        userRepository.save(user);
    }

    public void saveAlert(Integer userKey, String eventType, String level) {
        UserEntity user = userRepository.findById(userKey)
                .orElseThrow(() -> new UserNotFoundException("해당 유저가 존재하지 않습니다."));

        AlertHistoryEntity alert = new AlertHistoryEntity();
        alert.setUser(user);
        alert.setEventType(eventType);
        alert.setAlertLevel(level);
        alert.setDetectedAt(LocalDateTime.now());

        alertHistoryRepository.save(alert);
    }


}