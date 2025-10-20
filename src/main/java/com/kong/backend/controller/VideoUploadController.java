package com.kong.backend.controller;

import com.kong.backend.service.VideoStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/videos")
public class VideoUploadController {

    private final VideoStorageService storageService;

    @PostMapping(value = "/upload/{clipId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            @PathVariable String clipId,
            @RequestPart("file") MultipartFile file
    ) {
        if (!StringUtils.hasText(clipId) || file.isEmpty()) {
            return ResponseEntity.badRequest().body("clipId 또는 file 누락");
        }
        try {
            String savedPath = storageService.save(file, clipId);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedPath);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("업로드 실패: " + e.getMessage());
        }
    }
}
