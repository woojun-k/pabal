package com.polarishb.pabal.messenger.application.service;

import com.polarishb.pabal.messenger.application.command.SendableCommand;
import com.polarishb.pabal.messenger.application.command.output.SendMessageResult;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.contract.persistence.message.MessageState;
import com.polarishb.pabal.messenger.domain.model.Message;

import java.util.Optional;
import java.util.UUID;

public interface MessageSendSupport {

    MessageState loadReplyTarget(UUID tenantId, UUID replyToMessageId);

    void validateReplyTarget(MessageState replyTarget, UUID chatRoomId);

    Optional<MessageState> findDuplicate(SendableCommand command);

    MessageState loadDuplicate(SendableCommand command);

    MessageState send(PersistedChatRoom persistedChatRoom, Message message);

    SendMessageResult toDuplicateResult(MessageState message);

    SendMessageResult toSentResult(MessageState message);
}
