package com.polarishb.pabal.messenger.domain.exception;

import com.polarishb.pabal.messenger.domain.exception.code.MessengerErrorCode;

import java.util.UUID;

public class MemberAlreadyActiveException extends MessengerException {

    public MemberAlreadyActiveException() {
        super(MessengerErrorCode.MEMBER_ALREADY_ACTIVE);
    }

    public MemberAlreadyActiveException(String customMessage) {
        super(MessengerErrorCode.MEMBER_ALREADY_ACTIVE, customMessage);
    }

    public MemberAlreadyActiveException(UUID userId) {
        super(
                MessengerErrorCode.MEMBER_ALREADY_ACTIVE,
                MessengerErrorCode.MEMBER_ALREADY_ACTIVE.getMessage(),
                payload(entry("userId", userId))
        );
    }

}
