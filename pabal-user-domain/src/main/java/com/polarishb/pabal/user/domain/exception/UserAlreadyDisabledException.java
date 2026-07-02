package com.polarishb.pabal.user.domain.exception;

import com.polarishb.pabal.user.domain.exception.code.UserErrorCode;

public class UserAlreadyDisabledException extends UserException {

    public UserAlreadyDisabledException() {
        super(UserErrorCode.USER_ALREADY_DISABLED);
    }
}
