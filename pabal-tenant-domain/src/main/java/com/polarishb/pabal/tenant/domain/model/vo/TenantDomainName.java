package com.polarishb.pabal.tenant.domain.model.vo;

import com.polarishb.pabal.common.exception.InvalidInputException;

import java.net.IDN;
import java.util.Locale;

public record TenantDomainName(String value) {

    private static final int MAX_LENGTH = 253;

    public TenantDomainName {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidInputException("tenant domain은 필수입니다");
        }

        value = normalize(value);
        validate(value);
    }

    private static String normalize(String rawValue) {
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        try {
            return IDN.toASCII(normalized);
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException("tenant domain 형식이 올바르지 않습니다");
        }
    }

    private static void validate(String domainName) {
        if (domainName.length() > MAX_LENGTH
                || domainName.contains("://")
                || domainName.contains("/")
                || domainName.contains(":")) {
            throw new InvalidInputException("tenant domain 형식이 올바르지 않습니다");
        }

        String[] labels = domainName.split("\\.");
        if (labels.length < 2) {
            throw new InvalidInputException("tenant domain은 루트 domain을 포함해야 합니다");
        }

        for (String label : labels) {
            validateLabel(label);
        }
    }

    private static void validateLabel(String label) {
        if (label.isBlank() || label.length() > 63) {
            throw new InvalidInputException("tenant domain label 형식이 올바르지 않습니다");
        }
        if (label.startsWith("-") || label.endsWith("-")) {
            throw new InvalidInputException("tenant domain label은 hyphen으로 시작하거나 끝날 수 없습니다");
        }
        for (int i = 0; i < label.length(); i++) {
            char c = label.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-')) {
                throw new InvalidInputException("tenant domain label은 영문, 숫자, hyphen만 허용합니다");
            }
        }
    }
}
