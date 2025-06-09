package com.kong.backend.service;

import com.kong.backend.DTO.AlertCreateTestDto;
import com.kong.backend.DTO.AlertHistoryDto;
import com.kong.backend.Entity.AlertHistoryEntity;

import com.kong.backend.Entity.UserEntity;
import com.kong.backend.exception.UserNotFoundException;
import com.kong.backend.repository.AlertHistoryRepository;
import com.kong.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertService {
    private final UserRepository userRepository;
    private final AlertHistoryRepository alertHistoryRepository;

    public List<AlertHistoryDto> getAlertHistoryByUserKey(Integer userKey) {
        List<AlertHistoryEntity> alerts = alertHistoryRepository.findByUser_UserKeyOrderByDetectedAtDesc(userKey);

        return alerts.stream().map(entity -> {
            AlertHistoryDto dto = new AlertHistoryDto();
            dto.setAlertId(entity.getAlertId());
            dto.setEventType(entity.getEventType());
            dto.setAlertLevel(entity.getAlertLevel());
            dto.setDetectedAt(entity.getDetectedAt());
            return dto;
        }).collect(Collectors.toList());
    }

    public List<AlertHistoryDto> getAllAlertHistories() {
        List<AlertHistoryEntity> alerts = alertHistoryRepository.findAll(Sort.by(Sort.Direction.DESC, "detectedAt"));

        return alerts.stream().map(entity -> {
            AlertHistoryDto dto = new AlertHistoryDto();
            dto.setAlertId(entity.getAlertId());
            dto.setEventType(entity.getEventType());
            dto.setAlertLevel(entity.getAlertLevel());
            dto.setDetectedAt(entity.getDetectedAt());
            return dto;
        }).collect(Collectors.toList());
    }

    public void saveMultipleTestAlerts(List<AlertCreateTestDto> dtos) {
        for (AlertCreateTestDto dto : dtos) {
            UserEntity user = userRepository.findById(dto.getUserKey())
                    .orElseThrow(() -> new UserNotFoundException("유저 없음: " + dto.getUserKey()));

            AlertHistoryEntity alert = new AlertHistoryEntity();
            alert.setUser(user);
            alert.setEventType(dto.getEventType());
            alert.setAlertLevel(dto.getAlertLevel());
            alert.setDetectedAt(LocalDateTime.now());

            alertHistoryRepository.save(alert);
        }
    }
}
