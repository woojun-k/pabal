package com.polarishb.pabal.messenger.infrastructure.application;

import com.polarishb.pabal.common.event.DomainEventPublisher;
import com.polarishb.pabal.messenger.application.command.SendableCommand;
import com.polarishb.pabal.messenger.application.command.output.SendMessageResult;
import com.polarishb.pabal.messenger.application.port.out.persistence.ChatRoomSequenceRepository;
import com.polarishb.pabal.messenger.application.port.out.persistence.MessageRepository;
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

    private final MessageRepository messageRepository;
    private final ChatRoomSequenceRepository chatRoomSequenceRepository;
    private final DomainEventPublisher eventPublisher;

    @Override
    public PersistedMessage loadReplyTarget(UUID tenantId, UUID replyToMessageId) {
        return messageRepository.findByTenantIdAndId(tenantId, replyToMessageId)
                .orElseThrow(() -> new MessageNotFoundException(replyToMessageId));
    }

    @Override
    public void validateReplyTarget(Message replyTarget, UUID chatRoomId) {
        if (!replyTarget.getChatRoomId().equals(chatRoomId)) {
            throw new InvalidReplyTargetException(replyTarget.getId(), chatRoomId);
        }
    }

    @Override
    public Optional<PersistedMessage> findDuplicate(SendableCommand command) {
        return messageRepository.findByTenantIdAndChatRoomIdAndSenderIdAndClientMessageId(
                command.tenantId(),
                command.chatRoomId(),
                command.senderId(),
                command.clientMessageId()
        );
    }

    @Override
    public PersistedMessage loadDuplicate(SendableCommand command) {
        return findDuplicate(command)
                .orElseThrow(() -> new MessageNotFoundException(command.clientMessageId()));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PersistedMessage send(PersistedChatRoom persistedChatRoom, Message message) {
        long sequence = chatRoomSequenceRepository.allocateNextMessageSequence(
                persistedChatRoom.state().tenantId(),
                persistedChatRoom.state().id()
        );
        message.assignSequence(sequence);

        PersistedMessage saved = messageRepository.append(draft(message));

        chatRoomSequenceRepository.updateLastMessageSnapshot(
                saved.state().tenantId(),
                saved.state().chatRoomId(),
                saved.state().id(),
                saved.state().sequence(),
                saved.state().createdAt()
        );

        eventPublisher.publishAfterCommit(
                new MessageSentEvent(
                        saved.state().tenantId(),
                        saved.state().id(),
                        saved.state().chatRoomId(),
                        saved.state().senderId(),
                        saved.state().clientMessageId(),
                        saved.state().sequence(),
                        saved.state().content(),
                        saved.state().createdAt(),
                        saved.state().version()
                )
        );

        return saved;
    }

    @Override
    public SendMessageResult toDuplicateResult(PersistedMessage persistedMessage) {
        return new SendMessageResult(
                persistedMessage.state().id(),
                persistedMessage.state().sequence(),
                persistedMessage.state().clientMessageId(),
                persistedMessage.state().createdAt(),
                true
        );
    }

    @Override
    public SendMessageResult toSentResult(PersistedMessage persistedMessage) {
        return new SendMessageResult(
                persistedMessage.state().id(),
                persistedMessage.state().sequence(),
                persistedMessage.state().clientMessageId(),
                persistedMessage.state().createdAt(),
                false
        );
    }

    private PersistedMessage draft(Message message) {
        MessageState state = MessagePersistenceMapper.toState(message, null);
        return new PersistedMessage(message, state);
    }
}
