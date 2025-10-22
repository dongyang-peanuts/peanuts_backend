package com.kong.backend.DTO;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VideoListItemDto {
    private Integer videoId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String contentType;
    private LocalDateTime uploadedAt;
    private Boolean bookmarked; // 사용자별 북마크 여부
}
