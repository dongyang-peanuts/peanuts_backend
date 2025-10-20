package com.kong.backend.controller;

import com.kong.backend.service.DeviceControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/videos/commands")
public class SaveClipCommandController {

    private final DeviceControlService deviceControlService;

    /** 관리자 버튼이 호출하는 엔드포인트 */
    @PostMapping("/save-clip")
    public ResponseEntity<?> saveClip(
            @RequestParam(defaultValue = "0") int paKey,
            @RequestParam(defaultValue = "30") int durationSec,
            @RequestParam(defaultValue = "5") int preBufferSec,
            @RequestParam(defaultValue = "0") int postBufferSec
    ) {
        try {
            var ack = deviceControlService.sendSaveClip(paKey, durationSec, preBufferSec, postBufferSec);
            return ResponseEntity.ok(ack);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("SAVE_CLIP 실패: " + e.getMessage());
        }
    }
}
