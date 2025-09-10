package com.kong.backend.DTO;

import java.util.List;

public record PredictionUpdateResponse(
        Integer paKey,
        int total,
        int updated,
        int skipped,
        int failed,
        List<Result> results
) {
    public record Result(
            Integer predKey,
            String status,   // updated | skipped | failed
            String message
    ) {}
}
