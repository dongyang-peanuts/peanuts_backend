package com.kong.backend.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "user")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userKey;

    @Column(length = 100, nullable = false, unique = true)
    private String userEmail;

    @Column(length = 100, nullable = false)
    private String userPwd;

    @Column(length = 100)
    private String userAddr;

    @Column(length = 100)
    private String userNumber;

    @Column(length = 100)
    private String proNum;

    @Column(nullable = false)
    private Integer Auth; // 0: 일반 사용자, 1: 관리자

    @Column(nullable = false)
    private LocalDate signupDate;

    // 유저와 환자 연관관계
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PatientEntity> patients;

}
