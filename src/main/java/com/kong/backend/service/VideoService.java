package com.kong.backend.service;

import com.kong.backend.Entity.UserEntity;
import com.kong.backend.Entity.VideoEntity;
import com.kong.backend.repository.UserRepository;
import com.kong.backend.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepo;
    private final UserRepository userRepo;

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

    private String sha256(byte[] bytes) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] d = md.digest(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : d) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
