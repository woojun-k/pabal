package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.common.event.DomainEventPublisher;
import com.polarishb.pabal.messenger.application.command.input.MarkReadCommand;
import com.polarishb.pabal.messenger.application.port.out.time.ClockPort;
import com.polarishb.pabal.messenger.application.service.ChatRoomAccessSupport;
import com.polarishb.pabal.messenger.application.service.context.ChatRoomAccess;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;
import com.polarishb.pabal.messenger.contract.persistence.message.MessageState;
import com.polarishb.pabal.messenger.domain.event.MessageReadEvent;
import com.polarishb.pabal.messenger.domain.exception.MessageNotFoundException;
import com.polarishb.pabal.messenger.domain.model.ChatRoomMember;
import com.polarishb.pabal.messenger.application.port.out.persistence.ChatRoomMemberRepository;
import com.polarishb.pabal.messenger.application.port.out.persistence.MessageReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class MarkReadCommandHandler implements CommandHandler<MarkReadCommand, Void> {

    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final MessageReadRepository messageReadRepository;
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

        MessageState lastReadMessage = messageReadRepository.findByTenantIdAndChatRoomIdAndId(command.tenantId(), command.chatRoomId(), command.lastReadMessageId())
                .orElseThrow(() -> new MessageNotFoundException(command.lastReadMessageId()));

        long lastReadSequence = lastReadMessage.sequence();
        boolean readCursorAdvanced = chatRoomMember.wouldAdvanceLastReadCursorTo(lastReadSequence);

        Instant readAt = clockPort.now();
        ChatRoomMember updatedChatRoomMember = chatRoomMember.updateLastRead(
                command.lastReadMessageId(),
                lastReadSequence,
                readAt
        );

        if (updatedChatRoomMember == chatRoomMember) {
            return null;
        }

        PersistedChatRoomMember updatedMember = chatRoomMemberRepository.update(
                member.withMember(updatedChatRoomMember)
        );

        if (!readCursorAdvanced) {
            return null;
        }

        eventPublisher.publishAfterCommit(
            new MessageReadEvent(
                command.tenantId(),
                command.chatRoomId(),
                command.userId(),
                updatedMember.state().lastReadMessageId(),
                updatedMember.state().lastReadSequence(),
                updatedMember.state().lastReadAt(),
                updatedMember.state().version()
            )
        );

        return null;
    }
}
