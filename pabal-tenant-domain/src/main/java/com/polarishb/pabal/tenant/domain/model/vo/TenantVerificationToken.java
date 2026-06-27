package com.polarishb.pabal.tenant.domain.model.vo;

import com.polarishb.pabal.common.exception.InvalidInputException;

public record TenantVerificationToken(String value) {

    private static final int MIN_LENGTH = 32;
    private static final int MAX_LENGTH = 128;

    public TenantVerificationToken {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidInputException("domain 검증 token은 필수입니다");
        }
        value = value.trim();
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new InvalidInputException("domain 검증 token 길이가 올바르지 않습니다");
        }
        if (!value.matches("[A-Za-z0-9_-]+")) {
            throw new InvalidInputException("domain 검증 token 형식이 올바르지 않습니다");
        }
    }
}
