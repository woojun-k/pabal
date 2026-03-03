package com.polarishb.pabal.messenger.application.service;

import com.polarishb.pabal.common.event.DomainEventPublisher;
import com.polarishb.pabal.messenger.application.command.SendableCommand;
import com.polarishb.pabal.messenger.application.command.output.SendMessageResult;
import com.polarishb.pabal.messenger.application.service.context.SendContext;
import com.polarishb.pabal.messenger.domain.event.MessageSentEvent;
import com.polarishb.pabal.messenger.domain.exception.*;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoomMember;
import com.polarishb.pabal.messenger.domain.model.entity.Message;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomRepository;
import com.polarishb.pabal.messenger.domain.repository.MessageRepository;
import com.polarishb.pabal.messenger.domain.repository.result.MessageResult;
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
    private final DomainEventPublisher eventPublisher;

    public SendContext loadContext(SendableCommand command) {

        // ChatRoom 조회
        ChatRoom chatRoom = chatRoomRepository
            .findByTenantIdAndId(command.tenantId(), command.chatRoomId())
            .orElseThrow(() -> new ChatRoomNotFoundException(command.chatRoomId()));

        // ChatRoomMember 조회
        ChatRoomMember member = chatRoomMemberRepository
            .findByTenantIdAndChatRoomIdAndUserId(
                command.tenantId(),
                command.chatRoomId(),
                command.senderId()
            )
            .orElseThrow(() -> new MemberNotInRoomException(command.senderId()));

        // 멤버 검증
        validateMemberActive(member, command.senderId());

        return new SendContext(chatRoom, member);
    }

    public void validateMemberActive(ChatRoomMember member, UUID senderId) {
        if (!member.isActive()) {
            throw new MemberNotActiveException(senderId);
        }
    }

    public Message loadReplyTarget(UUID tenantId, UUID replyToMessageId) {
        return messageRepository.findByTenantIdAndId(tenantId, replyToMessageId)
                .orElseThrow(() -> new MessageNotFoundException(replyToMessageId));
    }

    public void validateReplyTarget(Message replyTarget, UUID chatRoomId) {
        if (!replyTarget.getChatRoomId().equals(chatRoomId)) {
            throw new InvalidReplyTargetException(replyTarget.getId(), chatRoomId);
        }
    }

    public Optional<SendMessageResult> findDuplicate(SendableCommand command) {
        return messageRepository.findByTenantIdAndChatRoomIdAndSenderIdAndClientMessageId(
                    command.tenantId(),
                    command.chatRoomId(),
                    command.senderId(),
                    command.clientMessageId()
                )
                .map(message -> new SendMessageResult(
                        message.getId(),
                        message.getClientMessageId(),
                        message.getCreatedAt(),
                        true
                ));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public SendMessageResult saveAndPublish(ChatRoom chatRoom, Message message) {
        MessageResult result = messageRepository.save(message);

        chatRoom.updateLastMessage(result.id(), result.createdAt());
        chatRoomRepository.save(chatRoom);

        eventPublisher.publishAfterCommit(
                new MessageSentEvent(
                        result.id(),
                        result.chatRoomId(),
                        result.senderId()
                )
        );

        return new SendMessageResult(
                result.id(),
                result.clientMessageId(),
                result.createdAt(),
                false
        );
    }
}
