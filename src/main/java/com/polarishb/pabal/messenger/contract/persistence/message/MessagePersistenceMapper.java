package com.polarishb.pabal.messenger.contract.persistence.message;

import com.polarishb.pabal.messenger.domain.model.entity.Message;

public final class MessagePersistenceMapper {

    private MessagePersistenceMapper() {
    }

    public static Message toDomain(MessageState state) {
        return Message.reconstitute(state.snapshot());
    }

    public static MessageState toState(Message message, Long version) {
        return new MessageState(message.snapshot(), version);
    }

    public static PersistedMessage toPersisted(MessageState state) {
        return new PersistedMessage(toDomain(state), state);
    }
}
