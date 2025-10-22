package com.kong.backend.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "videobookmark",
        uniqueConstraints = @UniqueConstraint(name="uk_user_video", columnNames={"user_key","video_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookmarkEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer bookmarkId;

    // 누가
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userKey", nullable = false)
    private UserEntity user;

    // 무엇을
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "videoId", nullable = false)
    private VideoEntity video;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
