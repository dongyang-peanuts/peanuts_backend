package com.kong.backend.controller;

import com.kong.backend.Entity.VideoEntity;
import com.kong.backend.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/videos")
public class VideoController {

    private final VideoService videoService;

    @Operation(
            summary = "동영상 업로드",
            description = "라즈베리파이(또는 클라이언트)에서 업로드한 동영상을 EC2 로컬 디스크에 저장하고 메타데이터를 반환합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "업로드 성공",
            content = @Content(schema = @Schema(implementation = VideoUploadResponse.class))
    )
    @ApiResponse(responseCode = "400", description = "업로드 실패")
    @ApiResponse(responseCode = "413", description = "파일이 너무 큼")
    @ApiResponse(responseCode = "500", description = "서버 오류")
    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<VideoUploadResponse> upload(
            @RequestParam("userKey")
            @Schema(description = "업로드 파일의 소유 유저키", example = "1")
            Integer userKey,

            // Swagger UI에서 파일 업로드 위젯을 띄우기 위한 핵심 포인트 ↓
            @RequestPart("file")
            @Schema(type = "string", format = "binary", description = "업로드할 동영상 파일")
            MultipartFile file
    ) throws Exception {
        VideoEntity v = videoService.upload(userKey, file);
        // VideoEntity의 PK가 Long이면 아래 타입도 Long으로 맞추세요.
        return ResponseEntity.ok(new VideoUploadResponse(v.getVideoId(), v.getFilePath()));
    }

    // PK가 Long이면 Long으로, Integer면 Integer로 맞추세요.
    public record VideoUploadResponse(
            @Schema(description = "저장된 비디오 ID", example = "987")
            Integer videoId,
            @Schema(description = "EC2 로컬 저장 경로", example = "/home/ubuntu/app/videos/2025/10/fall_20251023_010102.mp4")
            String filePath
    ) {}
}
