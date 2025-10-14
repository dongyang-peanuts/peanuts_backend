package com.kong.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class VideoStorageService {

    @Value("${video.storage.dir:/var/app/videos}")
    private String baseDir;

    public String save(MultipartFile file, String clipId) throws Exception {
        String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
        Path dir = Paths.get(baseDir, day);
        Files.createDirectories(dir);

        String filename = clipId + ".mp4";
        Path target = dir.resolve(filename);

        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return target.toAbsolutePath().toString();
    }
}
