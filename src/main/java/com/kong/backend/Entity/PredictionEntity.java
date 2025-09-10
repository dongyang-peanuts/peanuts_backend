package com.kong.backend.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "prediction",
        indexes = {
                @Index(name = "idx_prediction_patient", columnList = "paKey"),
                @Index(name = "idx_prediction_disease", columnList = "diseaseCode")
        },
        uniqueConstraints = {
                // 같은 환자·질병·예측시각 중복 방지
                @UniqueConstraint(name = "ux_patient_disease_time",
                        columnNames = {"paKey", "diseaseCode", "predictedAt"})
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PredictionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer predKey; // PK (Integer)

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paKey", nullable = false)
    private PatientEntity patient;

    @Column(nullable = false, length = 32)
    private String diseaseCode;

    @Column(nullable = false)
    private Double riskScore;

    @Column(nullable = false)
    private Instant predictedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }
}
