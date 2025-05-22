package com.kong.backend.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer adminKey;

    @Column(nullable = false, length = 100, unique = true)
    private String adminId;

    @Column(nullable = false, length = 100)
    private String adminPwd;

    @Column(nullable = false)
    private Integer Auth; // 1 = 관리자
}
