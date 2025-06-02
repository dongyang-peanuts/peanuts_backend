package com.kong.backend.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "patient")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PatientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer paKey;

//    @Column(nullable = false)
//    private Integer sociKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userKey", nullable = false)
    private UserEntity user;

    @Column(nullable = false, length = 100)
    private String paName;

    @Column(nullable = false, length = 100)
    private String paAddr;

    private Integer paAge;

    private Double paHei;

    private Double paWei;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PatientInfoEntity> infos;
}
