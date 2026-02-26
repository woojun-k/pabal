package com.polarishb.pabal.messenger.domain.exception;

import com.polarishb.pabal.messenger.domain.exception.code.MessengerErrorCode;

import java.util.Map;
import java.util.UUID;

public class MemberNotActiveException extends MessengerException {

    public MemberNotActiveException() {
        super(MessengerErrorCode.MEMBER_NOT_ACTIVE);
    }

    public MemberNotActiveException(String customMessage) {
        super(MessengerErrorCode.MEMBER_NOT_ACTIVE, customMessage);
    }

    public MemberNotActiveException(UUID userId) {
        super(
                MessengerErrorCode.MEMBER_NOT_ACTIVE,
                MessengerErrorCode.MEMBER_NOT_ACTIVE.getMessage(),
                Map.of("userId", userId.toString())
        );
    }

}
