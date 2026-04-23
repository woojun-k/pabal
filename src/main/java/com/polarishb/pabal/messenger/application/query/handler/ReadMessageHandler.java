package com.polarishb.pabal.messenger.application.query.handler;

import com.polarishb.pabal.common.cqrs.QueryHandler;
import com.polarishb.pabal.messenger.application.query.input.ReadMessageQuery;
import com.polarishb.pabal.messenger.application.query.mapper.MessageQueryMapper;
import com.polarishb.pabal.messenger.application.query.output.MessageDto;
import com.polarishb.pabal.messenger.application.service.ChatRoomReadAccessSupport;
import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;
import com.polarishb.pabal.messenger.domain.exception.MessageNotFoundException;
import com.polarishb.pabal.messenger.domain.repository.MessageReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReadMessageHandler implements QueryHandler<ReadMessageQuery, MessageDto> {

    private final MessageReadRepository messageReadRepository;
    private final MessageQueryMapper messageQueryMapper;
    private final ChatRoomReadAccessSupport chatRoomReadAccessSupport;

    @Override
    @Transactional(readOnly = true)
    public MessageDto handle(ReadMessageQuery query) {
        chatRoomReadAccessSupport.loadReadableActiveMember(
                query.tenantId(),
                query.chatRoomId(),
                query.userId()
        );

        PersistedMessage message = messageReadRepository.findByTenantIdAndChatRoomIdAndId(
                query.tenantId(),
                query.chatRoomId(),
                query.messageId()
        ).orElseThrow(() -> new MessageNotFoundException(query.messageId()));

        return messageQueryMapper.toMessageDto(message);
    }
}
