package com.kong.backend.service;

import com.kong.backend.Entity.BookmarkEntity;
import com.kong.backend.Entity.UserEntity;
import com.kong.backend.Entity.VideoEntity;
import com.kong.backend.exception.UserNotFoundException;
import com.kong.backend.repository.BookmarkRepository;
import com.kong.backend.repository.UserRepository;
import com.kong.backend.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepo;
    private final UserRepository userRepo;
    private final VideoRepository videoRepo;

    /** 토글: 있으면 삭제, 없으면 추가 → 결과(true=bookmarked) 반환 */
    @Transactional
    public boolean toggleBookmark(Integer userKey, Integer videoId) {
        var existing = bookmarkRepo.findByUser_UserKeyAndVideo_VideoId(userKey, videoId);
        if (existing.isPresent()) {
            bookmarkRepo.delete(existing.get());
            return false;
        }
        UserEntity user = userRepo.findById(userKey)
                .orElseThrow(() -> new UserNotFoundException("유저 없음: " + userKey));
        VideoEntity video = videoRepo.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("비디오 없음: " + videoId));

        BookmarkEntity newBm = BookmarkEntity.builder()
                .user(user)
                .video(video)
                .createdAt(LocalDateTime.now())
                .build();
        bookmarkRepo.save(newBm);
        return true;
    }
}
