package com.kong.backend.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "adminlogin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminLoginEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer adminloginKey;

    @Column(nullable = false)
    private String adminId;

    @Column(nullable = false)
    private LocalDateTime adminloginTime;

    @Column(nullable = false)
    private Integer Auth; // 0: 일반 사용자, 1: 관리자
}
