package com.kong.backend.repository;

import com.kong.backend.Entity.VideoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface VideoRepository extends JpaRepository<VideoEntity, Integer> {

    // ✅ 북마크 우선 → 최신 업로드 순 (주석 제거 + countQuery 추가)
    @Query(
            value = """
            SELECT v
              FROM VideoEntity v
              LEFT JOIN BookmarkEntity b
                     ON b.video = v AND b.user.userKey = :userKey
             WHERE v.user.userKey = :userKey
             ORDER BY CASE WHEN b.id IS NOT NULL THEN 1 ELSE 0 END DESC,
                      v.uploadedAt DESC
            """,
            countQuery = """
            SELECT COUNT(v)
              FROM VideoEntity v
              LEFT JOIN BookmarkEntity b
                     ON b.video = v AND b.user.userKey = :userKey
             WHERE v.user.userKey = :userKey
            """
    )
    Page<VideoEntity> findUserVideosOrderByBookmarkedThenUploadedAtDesc(
            @Param("userKey") Integer userKey,
            Pageable pageable
    );
}
