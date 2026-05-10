package com.polarishb.pabal.messenger.application.query.handler;

import com.polarishb.pabal.common.cqrs.QueryHandler;
import com.polarishb.pabal.messenger.application.query.input.GetUnreadCountQuery;
import com.polarishb.pabal.messenger.application.query.output.UnreadCountResult;
import com.polarishb.pabal.messenger.application.service.ChatRoomReadAccessSupport;
import com.polarishb.pabal.messenger.application.service.context.ChatRoomReadAccess;
import com.polarishb.pabal.messenger.application.port.out.persistence.MessageReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GetUnreadCountHandler implements QueryHandler<GetUnreadCountQuery, UnreadCountResult> {

    private final MessageReadRepository messageReadRepository;
    private final ChatRoomReadAccessSupport chatRoomReadAccessSupport;

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResult handle(GetUnreadCountQuery query) {
        ChatRoomReadAccess access = chatRoomReadAccessSupport.loadReadableActiveMember(
                query.tenantId(),
                query.chatRoomId(),
                query.userId()
        );

        long lastReadSequence = access.member().member().getLastReadSequence() != null
                ? access.member().member().getLastReadSequence()
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
