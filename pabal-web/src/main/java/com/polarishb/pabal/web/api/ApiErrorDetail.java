package com.polarishb.pabal.web.api;

public record ApiErrorDetail(
        String field,
        String reason
) {
}
