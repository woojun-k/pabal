package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.common.event.DomainEventPublisher;
import com.polarishb.pabal.messenger.application.command.input.MarkReadCommand;
import com.polarishb.pabal.messenger.application.port.out.time.ClockPort;
import com.polarishb.pabal.messenger.application.service.ChatRoomAccessSupport;
import com.polarishb.pabal.messenger.application.service.context.ChatRoomAccess;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;
import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;
import com.polarishb.pabal.messenger.domain.event.MessageReadEvent;
import com.polarishb.pabal.messenger.domain.exception.MessageNotFoundException;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoomMember;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberRepository;
import com.polarishb.pabal.messenger.domain.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class MarkReadCommandHandler implements CommandHandler<MarkReadCommand, Void> {

    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final MessageRepository messageRepository;
    private final ChatRoomAccessSupport chatRoomAccessSupport;
    private final DomainEventPublisher eventPublisher;
    private final ClockPort clockPort;

    @Transactional
    public Void handle(MarkReadCommand command) {
        ChatRoomAccess access = chatRoomAccessSupport.loadReadableActiveMember(
                command.tenantId(),
                command.chatRoomId(),
                command.userId()
        );
        PersistedChatRoomMember member = access.member();
        ChatRoomMember chatRoomMember = member.member();

        PersistedMessage lastReadMessage = messageRepository.findByTenantIdAndChatRoomIdAndId(command.tenantId(), command.chatRoomId(), command.lastReadMessageId())
                .orElseThrow(() -> new MessageNotFoundException(command.lastReadMessageId()));

        long lastReadSequence = lastReadMessage.state().sequence();
        boolean readCursorAdvanced = chatRoomMember.wouldAdvanceLastReadCursorTo(lastReadSequence);

        Instant readAt = clockPort.now();
        boolean lastReadUpdated = chatRoomMember.updateLastRead(
                command.lastReadMessageId(),
                lastReadSequence,
                readAt
        );

        if (!lastReadUpdated) {
            return null;
        }

        chatRoomMemberRepository.update(member);

        if (!readCursorAdvanced) {
            return null;
        }

        eventPublisher.publishAfterCommit(
            new MessageReadEvent(
                command.tenantId(),
                command.chatRoomId(),
                command.userId(),
                chatRoomMember.getLastReadMessageId(),
                chatRoomMember.getLastReadAt()
            )
        );

        return null;
    }
}
