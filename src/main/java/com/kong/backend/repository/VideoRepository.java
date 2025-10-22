package com.kong.backend.repository;

import com.kong.backend.Entity.VideoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<VideoEntity, Long> { }
