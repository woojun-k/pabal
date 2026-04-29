package com.polarishb.pabal.messenger.domain.exception;

import com.polarishb.pabal.messenger.domain.exception.code.MessengerErrorCode;

public class MessageAlreadyDeletedException extends MessengerException {
    public MessageAlreadyDeletedException() {
        super(MessengerErrorCode.MESSAGE_ALREADY_DELETED);
    }
}
