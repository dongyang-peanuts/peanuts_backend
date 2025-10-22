package com.kong.backend.controller;

import com.kong.backend.DTO.VideoListItemDto;
import com.kong.backend.service.VideoQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "동영상 API", description = "동영상 조회/검색")
@RestController
@RequiredArgsConstructor
@RequestMapping("/videos")
public class VideoQueryController {

    private final VideoQueryService videoQueryService;

    @Operation(summary = "사용자 동영상 목록", description = "업로드 시각 내림차순으로 페이징 목록을 반환")
    @GetMapping("/user/{userKey}")
    public ResponseEntity<Page<VideoListItemDto>> list(
            @PathVariable Integer userKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(videoQueryService.getUserVideos(userKey, page, size));
    }
}
