package com.polarishb.pabal.messenger.application.query.handler;

import com.polarishb.pabal.common.cqrs.QueryHandler;
import com.polarishb.pabal.messenger.application.query.input.ListMessagesQuery;
import com.polarishb.pabal.messenger.application.query.mapper.MessageQueryMapper;
import com.polarishb.pabal.messenger.application.query.output.MessageDto;
import com.polarishb.pabal.messenger.application.query.output.MessagePageDto;
import com.polarishb.pabal.messenger.application.service.ChatRoomReadAccessSupport;
import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomMemberReadRepository;
import com.polarishb.pabal.messenger.domain.repository.ChatRoomReadRepository;
import com.polarishb.pabal.messenger.domain.repository.MessageReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ListMessagesHandler implements QueryHandler<ListMessagesQuery, MessagePageDto> {

    private final ChatRoomReadRepository chatRoomReadRepository;
    private final ChatRoomMemberReadRepository chatRoomMemberReadRepository;
    private final MessageReadRepository messageReadRepository;
    private final MessageQueryMapper messageQueryMapper;
    private final ChatRoomReadAccessSupport chatRoomReadAccessSupport;

    @Override
    @Transactional(readOnly = true)
    public MessagePageDto handle(ListMessagesQuery query) {
        chatRoomReadAccessSupport.loadReadableActiveMember(
                query.tenantId(),
                query.chatRoomId(),
                query.userId()
        );

        int fetchSize = query.size() + 1;

        List<PersistedMessage> fetched = messageReadRepository.findByTenantIdAndChatRoomIdBeforeSequence(
                query.tenantId(),
                query.chatRoomId(),
                query.cursor(),
                fetchSize
        );

        boolean hasNext = fetched.size() > query.size();
        List<PersistedMessage> page = hasNext
                ? fetched.subList(0, query.size())
                : fetched;

        Long nextCursor = null;
        if (hasNext && !page.isEmpty()) {
            nextCursor = page.get(page.size() - 1).state().sequence();
        }

        List<MessageDto> messages = messageQueryMapper.toMessageDtosOldestFirst(page);

        Collections.reverse(messages);

        return new MessagePageDto(messages, nextCursor, hasNext);
    }
}
