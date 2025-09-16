package com.kong.backend.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(
        name = "fall_event",
        indexes = {
                @Index(name = "idx_fall_event_patient", columnList = "paKey"),
                @Index(name = "idx_fall_event_detectedAt", columnList = "detectedAt")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_fall_event_patient_time",
                        columnNames = {"paKey", "detectedAt"})
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class FallEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer fallKey;   // PK

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paKey", nullable = false)
    private com.kong.backend.Entity.PatientEntity patient;

    /** 낙상 심각도 (예: LOW, MEDIUM, HIGH, CRITICAL) */
    @Column(nullable = false, length = 16)
    private String severity;

    /** 낙상 발생 시각 (UTC 권장) */
    @Column(nullable = false)
    private Instant detectedAt;

    /** 레코드 생성 시각 */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
