package com.kong.backend.repository;

import com.kong.backend.Entity.AlertHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertHistoryRepository extends JpaRepository<AlertHistoryEntity, Integer> {
    List<AlertHistoryEntity> findByUser_UserKeyOrderByDetectedAtDesc(Integer userKey);
}
