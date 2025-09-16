package com.kong.backend.repository;

import com.kong.backend.Entity.FallEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface FallEventRepository extends JpaRepository<FallEventEntity, Integer> {

    boolean existsByPatient_PaKeyAndDetectedAt(Integer paKey, Instant detectedAt);

    Optional<FallEventEntity> findByPatient_PaKeyAndDetectedAt(Integer paKey, Instant detectedAt);

    Page<FallEventEntity> findByPatient_PaKey(Integer paKey, Pageable pageable);
}
