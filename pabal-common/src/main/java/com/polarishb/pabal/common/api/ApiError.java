package com.polarishb.pabal.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatusCode;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        String traceId,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<ApiErrorDetail> details
) {
    public ApiError {
        details = details == null ? List.of() : List.copyOf(details);
    }

    public static ApiError of(
            HttpStatusCode statusCode,
            String code,
            String message,
            String path,
            String traceId
    ) {
        return of(statusCode, code, message, path, traceId, List.of());
    }

    public static ApiError of(
            HttpStatusCode statusCode,
            String code,
            String message,
            String path,
            String traceId,
            List<ApiErrorDetail> details
    ) {
        return new ApiError(
                Instant.now(),
                statusCode.value(),
                code,
                message,
                path,
                traceId,
                details
        );
    }
}
