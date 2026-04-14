package com.polarishb.pabal.messenger.application.query.handler;

import com.polarishb.pabal.common.cqrs.QueryHandler;
import com.polarishb.pabal.messenger.application.query.input.GetUnreadCountQuery;
import com.polarishb.pabal.messenger.application.query.output.UnreadCountResult;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;
import com.polarishb.pabal.messenger.domain.exception.ChatRoomNotFoundException;
import com.polarishb.pabal.messenger.domain.exception.MemberNotActiveException;
import com.polarishb.pabal.messenger.domain.exception.MemberNotInRoomException;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberReadRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomReadRepository;
import com.polarishb.pabal.messenger.domain.repository.MessageReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GetUnreadCountHandler implements QueryHandler<GetUnreadCountQuery, UnreadCountResult> {

    private final ChatRoomReadRepository chatRoomReadRepository;
    private final ChatRoomMemberReadRepository chatRoomMemberReadRepository;
    private final MessageReadRepository messageReadRepository;

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResult handle(GetUnreadCountQuery query) {
        chatRoomReadRepository.findByTenantIdAndId(query.tenantId(), query.chatRoomId())
                .orElseThrow(() -> new ChatRoomNotFoundException(query.chatRoomId()));

        PersistedChatRoomMember member = chatRoomMemberReadRepository.findByTenantIdAndChatRoomIdAndUserId(
                query.tenantId(),
                query.chatRoomId(),
                query.userId()
        ).orElseThrow(() -> new MemberNotInRoomException(query.userId()));

        if (!member.member().isActive()) {
            throw new MemberNotActiveException(query.userId());
        }

        long lastReadSequence = member.member().getLastReadSequence() != null
                ? member.member().getLastReadSequence()
                : 0L;

        return new UnreadCountResult(
                messageReadRepository.countUnreadInRoom(
                        query.tenantId(),
                        query.chatRoomId(),
                        query.userId(),
                        lastReadSequence
                )
        );
    }
}
