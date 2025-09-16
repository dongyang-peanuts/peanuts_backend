package com.kong.backend.controller;

import com.kong.backend.DTO.*;
import com.kong.backend.service.FallEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "낙상 API", description = "낙상 이벤트 저장/조회/수정 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/falls")
public class FallEventController {

    private final FallEventService fallEventService;

    // 저장 (중복시 409)
    @Operation(
            summary = "낙상 데이터 저장",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = FallEventSaveRequest.class),
                            examples = @ExampleObject(name = "예시", value = """
                    {"paKey":123,"severity":"HIGH","detectedAt":"2025-09-10T07:00:00Z"}
                    """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "저장 성공",
                            content = @Content(schema = @Schema(implementation = FallEventSaveResponse.class))),
                    @ApiResponse(responseCode = "400", description = "입력 형식 오류",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "환자 없음",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "중복",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    @PostMapping
    public ResponseEntity<FallEventSaveResponse> save(@RequestBody FallEventSaveRequest req) {
        Integer key = fallEventService.save(req.paKey(), req.severity(), req.detectedAt());
        var body = new FallEventSaveResponse(key, req.paKey(), req.severity(), req.detectedAt());
        return ResponseEntity.status(201).body(body);
    }

    // 업서트 (있으면 갱신, 없으면 생성)
    @Operation(
            summary = "낙상 이벤트 업서트",
            description = "중복 기준: (paKey, detectedAt). 중복이면 severity를 갱신.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = FallEventSaveRequest.class),
                            examples = @ExampleObject(name = "예시", value = """
                    {"paKey":123,"severity":"MEDIUM","detectedAt":"2025-09-10T07:00:00Z"}
                    """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "업서트 성공",
                            content = @Content(schema = @Schema(implementation = FallEventUpsertResponse.class))),
                    @ApiResponse(responseCode = "400", description = "입력 형식 오류",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "환자 없음",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    @PostMapping("/upsert")
    public ResponseEntity<FallEventUpsertResponse> upsert(@RequestBody FallEventSaveRequest req) {
        var res = fallEventService.saveOrUpdate(req.paKey(), req.severity(), req.detectedAt());
        return ResponseEntity.ok(res);
    }

    // 조회: 특정 환자 페이징(항상 4개)
    @Operation(
            summary = "특정 환자 낙상 페이징 조회",
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = PageResponse.class))),
                    @ApiResponse(responseCode = "404", description = "환자/데이터 없음",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    @GetMapping("/patient/{paKey}")
    public ResponseEntity<PageResponse<FallEventDto>> findByPatientPaged(
            @PathVariable Integer paKey,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(required = false) String sort // 기본 detectedAt,desc
    ) {
        var pageResult = fallEventService.findByPatientPaged(paKey, page, sort);
        return ResponseEntity.ok(pageResult);
    }

    // 여러 건 동시 부분 수정 (같은 paKey)
    @Operation(
            summary = "낙상 데이터 수정",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = FallEventUpdateRequest.class),
                            examples = @ExampleObject(name = "예시", value = """
                    {
                      "paKey": 123,
                      "updates": [
                        { "fallKey": 45, "severity": "CRITICAL" },
                        { "fallKey": 46, "detectedAt": "2025-09-10T08:00:00Z" }
                      ]
                    }
                    """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "수정 결과 반환",
                            content = @Content(schema = @Schema(implementation = FallEventUpdateResponse.class))),
                    @ApiResponse(responseCode = "400", description = "입력 형식 오류",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "중복",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    @PatchMapping("/patient/{paKey}")
    public ResponseEntity<FallEventUpdateResponse> updateMany(
            @PathVariable Integer paKey,
            @RequestBody FallEventUpdateRequest request
    ) {
        if (!paKey.equals(request.paKey())) {
            return ResponseEntity.badRequest()
                    .body(new FallEventUpdateResponse(paKey, 0, 0, 0, 1,
                            java.util.List.of(new FallEventUpdateResponse.Result(null, "failed", "경로 paKey와 바디 paKey 불일치"))));
        }
        var result = fallEventService.updateMany(request);
        return ResponseEntity.ok(result);
    }
}
