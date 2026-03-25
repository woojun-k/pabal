    package com.polarishb.pabal.messenger.application.command.handler;

    import com.polarishb.pabal.common.cqrs.CommandHandler;
    import com.polarishb.pabal.common.event.DomainEventPublisher;
    import com.polarishb.pabal.messenger.application.command.input.MarkReadCommand;
    import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;
    import com.polarishb.pabal.messenger.domain.event.MessageReadEvent;
    import com.polarishb.pabal.messenger.domain.exception.ChatRoomNotFoundException;
    import com.polarishb.pabal.messenger.domain.exception.MemberNotFoundException;
    import com.polarishb.pabal.messenger.domain.exception.MessageNotFoundException;
    import com.polarishb.pabal.messenger.domain.model.entity.ChatRoomMember;
    import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberRepository;
    import com.polarishb.pabal.messenger.domain.repository.ChatRoomRepository;
    import com.polarishb.pabal.messenger.domain.repository.MessageRepository;
    import lombok.RequiredArgsConstructor;
    import org.springframework.stereotype.Component;

    import java.time.Instant;

    @Component
    @RequiredArgsConstructor
    public class MarkReadCommandHandler implements CommandHandler<MarkReadCommand, Void> {

        private final ChatRoomMemberRepository chatRoomMemberRepository;
        private final ChatRoomRepository chatRoomRepository;
        private final MessageRepository messageRepository;
        private final DomainEventPublisher eventPublisher;

        public Void handle(MarkReadCommand command) {
            chatRoomRepository.findByTenantIdAndId(command.tenantId(), command.chatRoomId())
                    .orElseThrow(() -> new ChatRoomNotFoundException(command.chatRoomId()));

            PersistedChatRoomMember member = chatRoomMemberRepository.findByTenantIdAndChatRoomIdAndUserId(command.tenantId(), command.chatRoomId(), command.userId())
                    .orElseThrow(() -> new MemberNotFoundException(command.userId()));

            messageRepository.findByTenantIdAndChatRoomIdAndId(command.tenantId(), command.chatRoomId(), command.lastReadMessageId())
                    .orElseThrow(() -> new MessageNotFoundException(command.lastReadMessageId()));

            ChatRoomMember chatRoomMember = member.member();

            Instant readAt = Instant.now();
            chatRoomMember.updateLastRead(command.lastReadMessageId(), readAt);

            chatRoomMemberRepository.update(member);

            eventPublisher.publishAfterCommit(
                new MessageReadEvent(
                    command.tenantId(),
                    command.chatRoomId(),
                    command.userId(),
                    command.lastReadMessageId(),
                    readAt
                )
            );

            return null;
        }
    }
