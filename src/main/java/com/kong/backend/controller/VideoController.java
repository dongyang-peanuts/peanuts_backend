package com.kong.backend.controller;

import com.kong.backend.Entity.VideoEntity;
import com.kong.backend.repository.VideoRepository;
import com.kong.backend.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@Tag(name = "동영상 API", description = "동영상 업로드 / 삭제 / URL")
@RestController
@RequiredArgsConstructor
@RequestMapping("/videos")
public class VideoController {

    private final VideoService videoService;
    private final VideoRepository videoRepository;

    @Value("${app.storage.local.base-dir:/home/ubuntu/app/videos}")
    private String baseDir;

    @Value("${app.media.public-base-url:http://kongback.kro.kr/media}")
    private String publicBaseUrl;

    @Operation(
            summary = "동영상 업로드",
            description = "클라이언트에서 업로드한 동영상을 EC2 로컬서버에 저장"
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

    @Operation(
            summary = "동영상 삭제",
            description = "EC2 로컬에 저장되어있는 동영상과, DB 데이터 삭제"
    )
    @ApiResponse(responseCode = "204", description = "삭제 성공")
    @ApiResponse(responseCode = "403", description = "권한 없음")
    @ApiResponse(responseCode = "404", description = "비디오 없음")
    @DeleteMapping("/{videoId}")
    public ResponseEntity<Void> deleteVideo(
            @PathVariable
            @Schema(description = "삭제할 비디오 ID", example = "42")
            Integer videoId,
            @RequestParam
            @Schema(description = "유저키", example = "1")
            Integer userKey
    ) throws Exception {
        videoService.deleteVideo(videoId, userKey);
        return ResponseEntity.noContent().build(); // 204
    }

    // ✅ Nginx가 서빙하는 재생 URL 발급
    @Operation(
            summary = "동영상 스트리밍 URL",
            description = "Nginx에서 스트리밍하는 동영상의 URL을 반환 "
    )
    @ApiResponse(
            responseCode = "200",
            description = "URL 발급 성공",
            content = @Content(schema = @Schema(implementation = VideoUrlResponse.class))
    )
    @ApiResponse(responseCode = "404", description = "존재하지 않는 동영상 ID")
    @GetMapping("/url/{videoId}")
    public ResponseEntity<VideoUrlResponse> getPublicUrl(
            @PathVariable
            @Schema(description = "조회할 동영상 ID", example = "42")
            Integer videoId
    ) {
        VideoEntity v = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found: " + videoId));

        // baseDir 기준 상대경로로 변환 → /media/** 로 붙이기
        Path base = Path.of(baseDir).normalize().toAbsolutePath();
        Path absolute = Path.of(v.getFilePath()).normalize().toAbsolutePath();
        if (!absolute.startsWith(base)) {
            throw new IllegalStateException("Illegal path outside baseDir");
        }

        String relative = base.relativize(absolute).toString().replace('\\', '/');
        String baseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
        String url = baseUrl + "/" + relative;

        return ResponseEntity.ok(new VideoUrlResponse(videoId, url));
    }

    @Schema(description = "동영상 재생 URL 응답 DTO")
    public record VideoUrlResponse(
            @Schema(description = "비디오 ID", example = "42") Integer videoId,
            @Schema(description = "Nginx를 통한 재생 URL",
                    example = "http://kongback.kro.kr/media/1761154135594_308377_small.mp4")
            String url
    ) {}

}
