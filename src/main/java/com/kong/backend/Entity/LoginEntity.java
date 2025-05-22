package com.kong.backend.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "login")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long loginId;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private LocalDateTime loginTime;

    @Column(nullable = false)
    private Integer Auth; // 0: 일반 사용자, 1: 관리자
}
