package com.kong.backend.repository;

import com.kong.backend.Entity.PatientInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientInfoRepository extends JpaRepository<PatientInfoEntity, Integer> {
}
