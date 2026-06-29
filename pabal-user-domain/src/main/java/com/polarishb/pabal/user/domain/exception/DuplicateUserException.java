package com.polarishb.pabal.user.domain.exception;

import com.polarishb.pabal.user.domain.exception.code.UserErrorCode;

import java.util.UUID;

public class DuplicateUserException extends UserException {

    public DuplicateUserException(UUID userId) {
        super(
                UserErrorCode.DUPLICATE_USER,
                UserErrorCode.DUPLICATE_USER.getMessage(),
                payload(entry("userId", userId))
        );
    }
}
