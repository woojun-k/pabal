package com.polarishb.pabal.messenger.domain.exception;

import com.polarishb.pabal.messenger.domain.exception.code.MessengerErrorCode;

import java.util.UUID;

public class InvalidDirectChatParticipantsException extends MessengerException {

    public InvalidDirectChatParticipantsException() {
        super(MessengerErrorCode.INVALID_DIRECT_CHAT_PARTICIPANTS);
    }

    public InvalidDirectChatParticipantsException(String customMessage) {
        super(MessengerErrorCode.INVALID_DIRECT_CHAT_PARTICIPANTS, customMessage);
    }

    public InvalidDirectChatParticipantsException(UUID requesterId, UUID participantId) {
        super(
                MessengerErrorCode.INVALID_DIRECT_CHAT_PARTICIPANTS,
                MessengerErrorCode.INVALID_DIRECT_CHAT_PARTICIPANTS.getMessage(),
                payload(
                        entry("requesterId", requesterId),
                        entry("participantId", participantId)
                )
        );
    }
}
