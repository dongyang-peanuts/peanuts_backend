package com.kong.backend.repository;

import com.kong.backend.Entity.LoginEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoginRepository extends JpaRepository<LoginEntity, Long> {
    Optional<LoginEntity> findTopByUserEmailOrderByLoginTimeDesc(String userEmail);
}
