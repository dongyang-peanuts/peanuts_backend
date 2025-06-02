package com.kong.backend.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "patientinfo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PatientInfoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer infoKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paKey", nullable = false)
    private PatientEntity patient;

    private Integer paFact;
    private Integer paPrct;

    @Column(length = 100)
    private String paDi;

    @Column(length = 100)
    private String paDise;

    @Column(length = 100)
    private String paExti;

    @Column(length = 100)
    private String paBest;

    @Column(length = 100)
    private String paMedi;
}
