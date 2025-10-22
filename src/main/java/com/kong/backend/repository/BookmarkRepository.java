package com.kong.backend.repository;

import com.kong.backend.Entity.BookmarkEntity;
import com.kong.backend.Entity.VideoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface BookmarkRepository extends JpaRepository<BookmarkEntity, Integer> {
    Optional<BookmarkEntity> findByUser_UserKeyAndVideo_VideoId(Integer userKey, Integer videoId);
    boolean existsByUser_UserKeyAndVideo_VideoId(Integer userKey, Integer videoId);
    List<BookmarkEntity> findByUser_UserKey(Integer userKey);
}
