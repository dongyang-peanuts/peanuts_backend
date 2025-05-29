package com.kong.backend.repository;

import com.kong.backend.Entity.AdminLoginEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminLoginRepository extends JpaRepository<AdminLoginEntity, Long> {
    Optional<AdminLoginEntity> findTopByAdminIdOrderByAdminloginTimeDesc(String adminId);
}
