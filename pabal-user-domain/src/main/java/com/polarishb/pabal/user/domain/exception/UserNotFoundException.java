package com.polarishb.pabal.user.domain.exception;

import com.polarishb.pabal.user.domain.exception.code.UserErrorCode;

import java.util.UUID;

public class UserNotFoundException extends UserException {

    public UserNotFoundException(UUID userId) {
        super(
                UserErrorCode.USER_NOT_FOUND,
                UserErrorCode.USER_NOT_FOUND.getMessage(),
                payload(entry("userId", userId))
        );
    }
}
