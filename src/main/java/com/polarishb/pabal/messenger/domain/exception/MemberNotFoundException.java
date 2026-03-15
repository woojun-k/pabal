package com.polarishb.pabal.messenger.domain.exception;

import com.polarishb.pabal.messenger.domain.exception.code.MessengerErrorCode;

import java.util.Map;
import java.util.UUID;

public class MemberNotFoundException extends MessengerException {

    public MemberNotFoundException() {
        super(MessengerErrorCode.MEMBER_NOT_FOUND);
    }

    public MemberNotFoundException(String customMessage) {
        super(MessengerErrorCode.MEMBER_NOT_FOUND, customMessage);
    }

    public MemberNotFoundException(UUID userId) {
        super(
                MessengerErrorCode.MEMBER_NOT_FOUND,
                MessengerErrorCode.MEMBER_NOT_FOUND.getMessage(),
                Map.of("userId", userId.toString())
        );
    }
}
