package com.kong.backend.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "video", indexes = {
        @Index(name = "idx_video_user", columnList = "userkey"),
        @Index(name = "idx_video_uploadedAt", columnList = "uploadedAt")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VideoEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer videoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userkey", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    private String fileName;   // 저장된 실제 파일명

    @Column(nullable = false)
    private String filePath;   // EC2 디스크 경로 (예: /var/app/videos/2025/10/xxx.mp4)

    @Column(nullable = false)
    private Long fileSize;

    @Column(length = 100)
    private String contentType;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Column(length = 128)
    private String checksum;   // 선택: 중복/멱등 체크
}
