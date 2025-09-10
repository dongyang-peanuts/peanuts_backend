package com.kong.backend.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "alerthistory")
@Getter
@Setter
public class AlertHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer alertId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userKey", nullable = false)
    private UserEntity user; // 감지 대상 사용자

    @Column(nullable = false)
    private String eventType; // 예: 낙상, 배회, 손떨림 등

    @Column(nullable = false)
    private String alertLevel; // 예: 주의, 경고, 비상

    @Column(nullable = false)
    private LocalDateTime detectedAt; // 감지 시각

    @Column(name = "video_path")
    private String videoPath;
}
