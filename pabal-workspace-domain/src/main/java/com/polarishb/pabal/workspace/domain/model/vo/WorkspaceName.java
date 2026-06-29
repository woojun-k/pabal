package com.polarishb.pabal.workspace.domain.model.vo;

import com.polarishb.pabal.common.exception.InvalidInputException;

public record WorkspaceName(String value) {

    private static final int MAX_LENGTH = 100;

    public WorkspaceName {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidInputException("workspace 이름은 필수입니다");
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new InvalidInputException("workspace 이름은 %d자 이하여야 합니다".formatted(MAX_LENGTH));
        }
    }
}
