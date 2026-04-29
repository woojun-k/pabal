package com.polarishb.pabal.common.api;

public record ApiErrorDetail(
        String field,
        String reason
) {
}
