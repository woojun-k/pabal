package com.polarishb.pabal.common.api;

import java.util.Map;

public record ErrorResponse(
        String code,
        String message,
        Map<String, Object> payload
) {
    public ErrorResponse(String code, String message) {
        this(code, message, null);
    }
}
