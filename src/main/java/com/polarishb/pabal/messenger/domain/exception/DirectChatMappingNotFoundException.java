package com.polarishb.pabal.messenger.domain.exception;

import com.polarishb.pabal.messenger.domain.exception.code.MessengerErrorCode;

public class DirectChatMappingNotFoundException extends MessengerException {
    public DirectChatMappingNotFoundException() {
        super(MessengerErrorCode.DIRECT_CHAT_MAPPING_NOT_FOUND);
    }
}
