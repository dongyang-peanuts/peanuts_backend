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

//     기존 데이터를 저장하는 위험도
//    @Column(nullable = false)
//    private String alertLevel; // 예: HIGH, MEDIUM, LOW, RECOVERY 등

    @Column(nullable = false)
    private LocalDateTime detectedAt; // 감지 시각

    @Column(name = "video_path")
    private String videoPath;

    /* ---- 새 필드 (낙상 모델 추가 데이터) ---- */

//    기존 데이터를 저장하는 현재 사람의 포즈
//    @Column(name = "pose")
//    private String pose; // stand | sit | lay | unknown

    @Column(name = "lay_rate")
    private Double layRate; // 0.0 ~ 1.0 (누워 있을 확률 등)

    @Column(name = "prob")
    private Double prob; // 0.0 ~ 1.0 (배회할 확률 등)

//    기존 데이터를 저장하는 낙상확인용 데이터
//    @Column(name = "fall")
//    private Boolean fall; // true | false

    @Column(name = "ts")
    private Double ts; // 원본 epoch timestamp (float)
}
