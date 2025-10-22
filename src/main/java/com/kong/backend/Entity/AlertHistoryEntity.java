package com.kong.backend.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "alerthistory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer alertId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userKey", nullable = false)
    private UserEntity user; // 감지 대상 사용자

    @Column(nullable = false)
    private String eventType; // 예: 낙상, 낙상 해제 등

    @Column(nullable = false)
    private LocalDateTime detectedAt; // 감지 시각

    @Column(name = "lay_rate")
    private Double layRate; // 0.0 ~ 1.0 (누워 있을 확률 등)

    @Column(name = "prob")
    private Double prob; // 0.0 ~ 1.0 (배회할 확률 등)

    @Column(name = "ts")
    private Double ts; // 원본 epoch timestamp (float)

    // --- 새: Video FK (권장) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "videoId")
    private VideoEntity video;
}
