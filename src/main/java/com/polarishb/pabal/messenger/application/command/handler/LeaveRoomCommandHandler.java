package com.polarishb.pabal.messenger.application.command.handler;

import com.polarishb.pabal.common.cqrs.CommandHandler;
import com.polarishb.pabal.common.event.DomainEventPublisher;
import com.polarishb.pabal.messenger.application.command.input.LeaveRoomCommand;
import com.polarishb.pabal.messenger.application.port.out.time.ClockPort;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;
import com.polarishb.pabal.messenger.domain.event.MemberLeftEvent;
import com.polarishb.pabal.messenger.domain.exception.ChatRoomNotFoundException;
import com.polarishb.pabal.messenger.domain.exception.MemberNotActiveException;
import com.polarishb.pabal.messenger.domain.exception.MemberNotFoundException;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoomMember;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LeaveRoomCommandHandler implements CommandHandler<LeaveRoomCommand, Void> {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final DomainEventPublisher eventPublisher;
    private final ClockPort clockPort;

    @Override
    @Transactional
    public Void handle(LeaveRoomCommand command) {

        chatRoomRepository.findByTenantIdAndId(command.tenantId(), command.chatRoomId())
                .orElseThrow(() -> new ChatRoomNotFoundException(command.chatRoomId()));

        PersistedChatRoomMember persistedMember = chatRoomMemberRepository.findByTenantIdAndChatRoomIdAndUserId(command.tenantId(), command.chatRoomId(), command.userId())
                .orElseThrow(() -> new MemberNotFoundException(command.userId()));

        ChatRoomMember member = persistedMember.member();

        if(!member.isActive()) {
            throw new MemberNotActiveException(command.userId());
        }

        member.leave(clockPort.now());

        chatRoomMemberRepository.update(persistedMember);

        MemberLeftEvent event = new MemberLeftEvent(command.tenantId(), command.chatRoomId(), command.userId());

        eventPublisher.publishAfterCommit(event);

        return null;
    }
}
