package com.kong.backend.controller;

import com.kong.backend.repository.BookmarkRepository;
import com.kong.backend.service.BookmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "동영상 API", description = "동영상 북마크")
@RestController
@RequiredArgsConstructor
@RequestMapping("/bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final BookmarkRepository bookmarkRepo;

    @Operation(summary = "동영상 북마크", description = "해당 동영상에 대한 북마크를 추가 및 취소")
    @PostMapping("/videos/{videoId}")
    public ResponseEntity<BookmarkToggleResponse> toggle(
            @RequestParam Integer userKey,
            @PathVariable Integer videoId
    ) {
        boolean bookmarked = bookmarkService.toggleBookmark(userKey, videoId);
        return ResponseEntity.ok(new BookmarkToggleResponse(videoId, bookmarked));
    }

    public record BookmarkToggleResponse(Integer videoId, boolean bookmarked) {}
}
