package com.kong.backend.Entity;

import jakarta.persistence.*;
import lombok.*;

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
}
