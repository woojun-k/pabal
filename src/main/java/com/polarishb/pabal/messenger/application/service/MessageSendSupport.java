package com.polarishb.pabal.messenger.application.service;

import com.polarishb.pabal.common.event.DomainEventPublisher;
import com.polarishb.pabal.messenger.application.command.SendableCommand;
import com.polarishb.pabal.messenger.application.command.output.SendMessageResult;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;
import com.polarishb.pabal.messenger.contract.persistence.message.MessagePersistenceMapper;
import com.polarishb.pabal.messenger.contract.persistence.message.MessageState;
import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;
import com.polarishb.pabal.messenger.domain.event.MessageSentEvent;
import com.polarishb.pabal.messenger.domain.exception.*;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoomMember;
import com.polarishb.pabal.messenger.domain.model.entity.Message;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomSequenceRepository;
import com.polarishb.pabal.messenger.domain.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MessageSendSupport {

    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomSequenceRepository chatRoomSequenceRepository;
    private final DomainEventPublisher eventPublisher;

    public PersistedChatRoom loadChatRoom(SendableCommand command) {
        PersistedChatRoom persistedChatRoom = chatRoomRepository.findByTenantIdAndId(
                command.tenantId(),
                command.chatRoomId()
        ).orElseThrow(() -> new ChatRoomNotFoundException(command.chatRoomId()));

        persistedChatRoom.chatRoom().validateCanSend();
        return persistedChatRoom;
    }

    public PersistedChatRoomMember loadSenderMember(SendableCommand command) {
        return chatRoomMemberRepository.findByTenantIdAndChatRoomIdAndUserId(
                command.tenantId(),
                command.chatRoomId(),
                command.senderId()
        ).orElseThrow(() -> new MemberNotInRoomException(command.senderId()));
    }

    public void validateMemberActive(ChatRoomMember member, UUID senderId) {
        if (!member.isActive()) {
            throw new MemberNotActiveException(senderId);
        }
    }

    public PersistedMessage loadReplyTarget(UUID tenantId, UUID replyToMessageId) {
        return messageRepository.findByTenantIdAndId(tenantId, replyToMessageId)
                .orElseThrow(() -> new MessageNotFoundException(replyToMessageId));
    }

    public void validateReplyTarget(Message replyTarget, UUID chatRoomId) {
        if (!replyTarget.getChatRoomId().equals(chatRoomId)) {
            throw new InvalidReplyTargetException(replyTarget.getId(), chatRoomId);
        }
    }

    public Optional<PersistedMessage> findDuplicate(SendableCommand command) {
        return messageRepository.findByTenantIdAndChatRoomIdAndSenderIdAndClientMessageId(
                    command.tenantId(),
                    command.chatRoomId(),
                    command.senderId(),
                    command.clientMessageId()
                );
    }

    @Transactional(propagation = Propagation.MANDATORY)
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
                        saved.state().senderId()
                )
        );

        return saved;
    }

    private PersistedMessage draft(Message message) {
        MessageState state = MessagePersistenceMapper.toState(message, null);
        return new PersistedMessage(message, state);
    }

    public SendMessageResult toDuplicateResult(PersistedMessage persistedMessage) {
        return new SendMessageResult(
                persistedMessage.state().id(),
                persistedMessage.state().clientMessageId(),
                persistedMessage.state().createdAt(),
                true
        );
    }

    public SendMessageResult toSentResult(PersistedMessage persistedMessage) {
        return new SendMessageResult(
                persistedMessage.state().id(),
                persistedMessage.state().clientMessageId(),
                persistedMessage.state().createdAt(),
                false
        );
    }
}
