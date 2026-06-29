package com.polarishb.pabal.user.domain.model.vo;

import com.polarishb.pabal.common.exception.InvalidInputException;

public record UserName(String value) {

    private static final int MAX_LENGTH = 100;

    public UserName {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidInputException("사용자 이름은 필수입니다");
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new InvalidInputException(
                    String.format("사용자 이름은 %d자 이하여야 합니다", MAX_LENGTH)
            );
        }
    }
}
