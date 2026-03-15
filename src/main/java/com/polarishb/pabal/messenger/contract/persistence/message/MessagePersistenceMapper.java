package com.polarishb.pabal.messenger.contract.persistence.message;

import com.polarishb.pabal.messenger.domain.model.entity.Message;
import com.polarishb.pabal.messenger.domain.model.vo.MessageContent;

public final class MessagePersistenceMapper {

    private MessagePersistenceMapper() {
    }

    public static Message toDomain(MessageState state) {
        return Message.reconstitute(
                state.id(),
                state.tenantId(),
                state.chatRoomId(),
                state.senderId(),
                state.clientMessageId(),
                state.type(),
                new MessageContent(state.content()),
                state.status(),
                state.replyToMessageId(),
                state.createdAt(),
                state.updatedAt(),
                state.deletedAt()
        );
    }

    public static MessageState toState(Message message, Long version) {
        return new MessageState(
                message.getId(),
                message.getTenantId(),
                message.getChatRoomId(),
                message.getSenderId(),
                message.getClientMessageId(),
                message.getType(),
                message.getContent().value(),
                message.getStatus(),
                message.getReplyToMessageId(),
                message.getCreatedAt(),
                message.getUpdatedAt(),
                message.getDeletedAt(),
                version
        );
    }

    public static PersistedMessage toPersisted(MessageState state) {
        return new PersistedMessage(toDomain(state), state);
    }
}