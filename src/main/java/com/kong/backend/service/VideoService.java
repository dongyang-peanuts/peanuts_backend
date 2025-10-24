package com.kong.backend.service;

import com.kong.backend.Entity.UserEntity;
import com.kong.backend.Entity.VideoEntity;
import com.kong.backend.repository.AlertHistoryRepository;
import com.kong.backend.repository.BookmarkRepository;
import com.kong.backend.repository.UserRepository;
import com.kong.backend.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepo;
    private final UserRepository userRepo;
    private final BookmarkRepository bookmarkRepo;
    private final AlertHistoryRepository alertRepo;

    @Value("${app.storage.local.base-dir:/home/ubuntu/app/videos}")
    private String basePath;

    @Transactional
    public VideoEntity upload(Integer userKey, MultipartFile file) throws Exception {
        UserEntity user = userRepo.findById(userKey)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userKey));

        Path dir = Path.of(basePath);
        Files.createDirectories(dir);

        String original = file.getOriginalFilename() == null ? "video.mp4" : file.getOriginalFilename();
        String filename = System.currentTimeMillis() + "_" + original.replaceAll("\\s+", "_");
        Path target = dir.resolve(filename);

        Files.write(target, file.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        if (!Files.exists(target)) {
            throw new IllegalStateException("파일이 실제로 저장되지 않았습니다: " + target);
        }

        log.info("✅ 파일 저장 완료: {}", target.toAbsolutePath());

        String checksum = sha256(file.getBytes());

        VideoEntity saved = videoRepo.save(
                VideoEntity.builder()
                        .user(user)
                        .fileName(filename)
                        .filePath(target.toAbsolutePath().toString())
                        .fileSize(file.getSize())
                        .contentType(file.getContentType())
                        .uploadedAt(LocalDateTime.now())
                        .checksum(checksum)
                        .build()
        );

        log.info("✅ DB 저장 완료: videoId={}, userKey={}", saved.getVideoId(), userKey);
        return saved;
    }

    @Transactional
    public void deleteVideo(Integer videoId, Integer requestUserKey) throws IOException {
        VideoEntity v = videoRepo.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found: " + videoId));

        // ✅ 소유자 검증
        if (!v.getUser().getUserKey().equals(requestUserKey)) {
            throw new SecurityException("본인 소유 영상만 삭제할 수 있습니다.");
        }

        // ✅ 1) 알림에서 FK 분리
        alertRepo.detachVideoFromAlerts(videoId);

        // ✅ 2) 북마크 전부 삭제
        bookmarkRepo.deleteByVideo_VideoId(videoId);

        // ✅ 3) 비디오 엔티티 삭제
        videoRepo.delete(v);

        // ✅ 4) 실제 파일 삭제 (항상 수행)
        Path base = Path.of(basePath).normalize().toAbsolutePath();
        Path abs = Path.of(v.getFilePath()).normalize().toAbsolutePath();

        if (abs.startsWith(base)) {
            Files.deleteIfExists(abs);
            System.out.println("🗑️ 영상 파일 삭제 완료: " + abs);
        } else {
            System.out.println("⚠️ 해당 파일이 폴더에 없음: " + abs);
        }
    }

    private String sha256(byte[] bytes) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] d = md.digest(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : d) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
