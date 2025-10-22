// VideoQueryService.java
package com.kong.backend.service;

import com.kong.backend.DTO.VideoListItemDto;
import com.kong.backend.Entity.VideoEntity;
import com.kong.backend.repository.BookmarkRepository;
import com.kong.backend.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoQueryService {

    private final VideoRepository videoRepo;
    private final BookmarkRepository bookmarkRepo;

    /** ✅ 북마크 우선 + 최신순(페이지네이션 지원) */
    @Transactional(readOnly = true)
    public Page<VideoListItemDto> getUserVideos(Integer userKey, int page, int size) {
        Pageable pageable = PageRequest.of(page, size); // 정렬은 JPQL에서 처리
        Page<VideoEntity> vids = videoRepo.findUserVideosOrderByBookmarkedThenUploadedAtDesc(userKey, pageable);

        // 해당 사용자의 북마크된 videoId 집합
        Set<Integer> bookmarkedIds = bookmarkRepo.findByUser_UserKey(userKey).stream()
                .map(b -> b.getVideo().getVideoId())
                .collect(Collectors.toSet());

        return vids.map(v -> {
            var dto = new VideoListItemDto();
            dto.setVideoId(v.getVideoId());
            dto.setFileName(v.getFileName());
            dto.setFilePath(v.getFilePath());
            dto.setFileSize(v.getFileSize());
            dto.setContentType(v.getContentType());
            dto.setUploadedAt(v.getUploadedAt());
            dto.setBookmarked(bookmarkedIds.contains(v.getVideoId())); // 표시
            return dto;
        });
    }
}
