package com.kong.backend.service;

import com.kong.backend.DTO.AdminDto;
import com.kong.backend.DTO.PatientDto;
import com.kong.backend.DTO.PatientInfoDto;
import com.kong.backend.DTO.UserDto;
import com.kong.backend.Entity.*;
import com.kong.backend.exception.AdminNotFoundException;
import com.kong.backend.exception.PasswordMismatchException;
import com.kong.backend.exception.UserNotFoundException;
import com.kong.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final AdminLoginRepository adminLoginRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PatientInfoRepository patientInfoRepository;

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

    // 사용자 정보조회
    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    public List<UserDto> getAllUsersWithDetails() {
        List<UserEntity> users = userRepository.findAll();
        List<UserDto> userDtos = new ArrayList<>();

        for (UserEntity user : users) {
            UserDto userDto = new UserDto();
            userDto.setUserKey(user.getUserKey());
            userDto.setUserEmail(user.getUserEmail());
            userDto.setUserAddr(user.getUserAddr());
            userDto.setUserNumber(user.getUserNumber());
            userDto.setSignupDate(user.getSignupDate());
            userDto.setProNum(user.getProNum());

            userDtos.add(userDto);
        }

        return userDtos;
    }

    // 사용자 정보 상세조회
    public UserDto getUserDetails(Integer userKey) {
        UserEntity user = userRepository.findById(userKey)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        UserDto userDto = new UserDto();
        userDto.setUserKey(user.getUserKey());
        userDto.setUserEmail(user.getUserEmail());
        userDto.setUserPwd(null); // 보안상 비밀번호는 반환 X
        userDto.setUserAddr(user.getUserAddr());
        userDto.setUserNumber(user.getUserNumber());
        userDto.setProNum(user.getProNum());

        List<PatientDto> patientDtos = new ArrayList<>();
        for (PatientEntity patient : user.getPatients()) {
            PatientDto patientDto = new PatientDto();
            patientDto.setPaName(patient.getPaName());
            patientDto.setPaAddr(patient.getPaAddr());
            patientDto.setPaAge(patient.getPaAge());
            patientDto.setPaHei(patient.getPaHei());
            patientDto.setPaWei(patient.getPaWei());

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

            patientDto.setInfos(infoDtos);
            patientDtos.add(patientDto);
        }

        userDto.setPatients(patientDtos);
        return userDto;
    }

    @Transactional
    public void deleteUserByKey(Integer userKey) {
        UserEntity user = userRepository.findById(userKey)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        // 환자 정보 먼저 삭제 → 연관된 환자 → 유저 삭제
        for (PatientEntity patient : user.getPatients()) {
            patientInfoRepository.deleteAll(patient.getInfos());
        }
        patientRepository.deleteAll(user.getPatients());

        userRepository.delete(user);
    }
}
