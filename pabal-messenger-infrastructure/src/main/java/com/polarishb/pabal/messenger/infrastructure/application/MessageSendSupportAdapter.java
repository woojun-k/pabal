package com.polarishb.pabal.messenger.infrastructure.application;

import com.polarishb.pabal.common.event.DomainEventPublisher;
import com.polarishb.pabal.messenger.application.command.SendableCommand;
import com.polarishb.pabal.messenger.application.command.output.SendMessageResult;
import com.polarishb.pabal.messenger.application.port.out.persistence.MessageReadRepository;
import com.polarishb.pabal.messenger.application.port.out.persistence.MessageWriteRepository;
import com.polarishb.pabal.messenger.application.port.out.persistence.ChatRoomSequenceRepository;
import com.polarishb.pabal.messenger.application.service.MessageSendSupport;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.contract.persistence.message.MessagePersistenceMapper;
import com.polarishb.pabal.messenger.contract.persistence.message.MessageState;
import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;
import com.polarishb.pabal.messenger.domain.event.MessageSentEvent;
import com.polarishb.pabal.messenger.domain.exception.InvalidReplyTargetException;
import com.polarishb.pabal.messenger.domain.exception.MessageNotFoundException;
import com.polarishb.pabal.messenger.domain.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MessageSendSupportAdapter implements MessageSendSupport {

    private final MessageReadRepository messageReadRepository;
    private final MessageWriteRepository messageWriteRepository;
    private final ChatRoomSequenceRepository chatRoomSequenceRepository;
    private final DomainEventPublisher eventPublisher;

    @Override
    public MessageState loadReplyTarget(UUID tenantId, UUID replyToMessageId) {
        return messageReadRepository.findByTenantIdAndId(tenantId, replyToMessageId)
                .orElseThrow(() -> new MessageNotFoundException(replyToMessageId));
    }

    @Override
    public void validateReplyTarget(MessageState replyTarget, UUID chatRoomId) {
        if (!replyTarget.chatRoomId().equals(chatRoomId)) {
            throw new InvalidReplyTargetException(replyTarget.id(), chatRoomId);
        }
    }

    @Override
    public Optional<MessageState> findDuplicate(SendableCommand command) {
        return messageReadRepository.findByTenantIdAndChatRoomIdAndSenderIdAndClientMessageId(
                command.tenantId(),
                command.chatRoomId(),
                command.senderId(),
                command.clientMessageId()
        );
    }

    @Override
    public MessageState loadDuplicate(SendableCommand command) {
        return findDuplicate(command)
                .orElseThrow(() -> new MessageNotFoundException(command.clientMessageId()));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MessageState send(PersistedChatRoom persistedChatRoom, Message message) {
        long sequence = chatRoomSequenceRepository.allocateNextMessageSequence(
                persistedChatRoom.state().tenantId(),
                persistedChatRoom.state().id()
        );
        Message sequenced = message.assignSequence(sequence);

        MessageState saved = messageWriteRepository.append(draft(sequenced));

        chatRoomSequenceRepository.updateLastMessageSnapshot(
                saved.tenantId(),
                saved.chatRoomId(),
                saved.id(),
                saved.sequence(),
                saved.createdAt()
        );

        eventPublisher.publishAfterCommit(
                new MessageSentEvent(
                        saved.tenantId(),
                        saved.id(),
                        saved.chatRoomId(),
                        saved.senderId(),
                        saved.clientMessageId(),
                        saved.sequence(),
                        saved.content(),
                        saved.createdAt(),
                        saved.version()
                )
        );

        return saved;
    }

    @Override
    public SendMessageResult toDuplicateResult(MessageState message) {
        return new SendMessageResult(
                message.id(),
                message.sequence(),
                message.clientMessageId(),
                message.createdAt(),
                true
        );
    }

    @Override
    public SendMessageResult toSentResult(MessageState message) {
        return new SendMessageResult(
                message.id(),
                message.sequence(),
                message.clientMessageId(),
                message.createdAt(),
                false
        );
    }

    private PersistedMessage draft(Message message) {
        MessageState state = MessagePersistenceMapper.toState(message, null);
        return new PersistedMessage(message, state);
    }
}
