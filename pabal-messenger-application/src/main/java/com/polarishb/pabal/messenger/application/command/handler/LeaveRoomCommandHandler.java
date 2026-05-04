package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.common.event.DomainEventPublisher;
import com.polarishb.pabal.messenger.application.command.input.LeaveRoomCommand;
import com.polarishb.pabal.messenger.application.port.out.time.ClockPort;
import com.polarishb.pabal.messenger.application.service.ChatRoomAccessSupport;
import com.polarishb.pabal.messenger.application.service.context.ChatRoomAccess;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;
import com.polarishb.pabal.messenger.domain.event.MemberLeftEvent;
import com.polarishb.pabal.messenger.domain.model.ChatRoomMember;
import com.polarishb.pabal.messenger.application.port.out.persistence.ChatRoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LeaveRoomCommandHandler implements CommandHandler<LeaveRoomCommand, Void> {

    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomAccessSupport chatRoomAccessSupport;
    private final DomainEventPublisher eventPublisher;
    private final ClockPort clockPort;

    @Override
    @Transactional
    public Void handle(LeaveRoomCommand command) {

        ChatRoomAccess access = chatRoomAccessSupport.loadLeavableMember(
                command.tenantId(),
                command.chatRoomId(),
                command.userId()
        );
        PersistedChatRoomMember persistedMember = access.member();
        ChatRoomMember member = persistedMember.member();

        member.leave(clockPort.now());

        PersistedChatRoomMember updatedMember = chatRoomMemberRepository.update(persistedMember);
        long sequence = access.room().state().lastMessageSequence() != null
                ? access.room().state().lastMessageSequence()
                : 0L;

        MemberLeftEvent event = new MemberLeftEvent(
                command.tenantId(),
                command.chatRoomId(),
                command.userId(),
                sequence,
                updatedMember.state().leftAt(),
                updatedMember.state().version()
        );

        eventPublisher.publishAfterCommit(event);

        return null;
    }
}
