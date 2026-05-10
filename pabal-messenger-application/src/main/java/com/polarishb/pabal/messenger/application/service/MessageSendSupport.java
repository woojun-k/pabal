package com.polarishb.pabal.messenger.application.service;

import com.polarishb.pabal.messenger.application.command.SendableCommand;
import com.polarishb.pabal.messenger.application.command.output.SendMessageResult;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;
import com.polarishb.pabal.messenger.domain.model.Message;

import java.util.Optional;
import java.util.UUID;

public interface MessageSendSupport {

    PersistedMessage loadReplyTarget(UUID tenantId, UUID replyToMessageId);

    void validateReplyTarget(Message replyTarget, UUID chatRoomId);

    Optional<PersistedMessage> findDuplicate(SendableCommand command);

    PersistedMessage loadDuplicate(SendableCommand command);

    PersistedMessage send(PersistedChatRoom persistedChatRoom, Message message);

    SendMessageResult toDuplicateResult(PersistedMessage persistedMessage);

    SendMessageResult toSentResult(PersistedMessage persistedMessage);
}
