package com.polarishb.pabal.messenger.application.query.handler;

import com.polarishb.pabal.common.cqrs.QueryHandler;
import com.polarishb.pabal.messenger.application.query.input.ReadMessageQuery;
import com.polarishb.pabal.messenger.application.query.output.MessageDto;
import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;
import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;
import com.polarishb.pabal.messenger.domain.exception.ChatRoomNotFoundException;
import com.polarishb.pabal.messenger.domain.exception.MemberNotActiveException;
import com.polarishb.pabal.messenger.domain.exception.MemberNotInRoomException;
import com.polarishb.pabal.messenger.domain.exception.MessageNotFoundException;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberReadRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomReadRepository;
import com.polarishb.pabal.messenger.domain.repository.MessageReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReadMessageHandler implements QueryHandler<ReadMessageQuery, MessageDto> {

    private final ChatRoomReadRepository chatRoomReadRepository;
    private final ChatRoomMemberReadRepository chatRoomMemberReadRepository;
    private final MessageReadRepository messageReadRepository;

    @Override
    @Transactional(readOnly = true)
    public MessageDto handle(ReadMessageQuery query) {
        PersistedChatRoom room = chatRoomReadRepository.findByTenantIdAndId(query.tenantId(), query.chatRoomId())
                .orElseThrow(() -> new ChatRoomNotFoundException(query.chatRoomId()));

        room.chatRoom().validateCanRead();

        PersistedChatRoomMember member = chatRoomMemberReadRepository.findByTenantIdAndChatRoomIdAndUserId(
                query.tenantId(),
                query.chatRoomId(),
                query.userId()
        ).orElseThrow(() -> new MemberNotInRoomException(query.userId()));

        if (!member.member().isActive()) {
            throw new MemberNotActiveException(query.userId());
        }

        PersistedMessage message = messageReadRepository.findByTenantIdAndChatRoomIdAndId(
                query.tenantId(),
                query.chatRoomId(),
                query.messageId()
        ).orElseThrow(() -> new MessageNotFoundException(query.messageId()));

        return new MessageDto(
                message.state().id(),
                message.state().chatRoomId(),
                message.state().senderId(),
                message.state().clientMessageId(),
                message.state().content(),
                message.state().status().name(),
                message.state().replyToMessageId(),
                message.state().createdAt(),
                message.state().updatedAt(),
                message.state().deletedAt()
        );
    }
}
