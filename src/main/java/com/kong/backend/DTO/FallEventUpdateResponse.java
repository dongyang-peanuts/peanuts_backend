package com.kong.backend.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "낙상 여러 건 부분 수정 결과")
public record FallEventUpdateResponse(
        @Schema(example = "123") Integer paKey,
        @Schema(example = "2") int total,
        @Schema(example = "2") int updated,
        @Schema(example = "0") int skipped,
        @Schema(example = "0") int failed,
        List<Result> results
) {
    public record Result(
            @Schema(example = "45") Integer fallKey,
            @Schema(example = "updated", allowableValues = {"updated","skipped","failed"}) String status,
            @Schema(example = "수정 완료") String message
    ) {}
}
