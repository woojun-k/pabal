package com.polarishb.pabal.messenger.application.query.mapper;

import com.polarishb.pabal.messenger.application.query.output.MessageDto;
import com.polarishb.pabal.messenger.contract.persistence.message.MessageState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MessageQueryMapper {

    public MessageDto toMessageDto(MessageState message) {
        return new MessageDto(
                message.id(),
                message.chatRoomId(),
                message.senderId(),
                message.clientMessageId(),
                message.sequence(),
                message.content(),
                message.status().name(),
                message.replyToMessageId(),
                message.createdAt(),
                message.updatedAt(),
                message.deletedAt()
        );
    }

    public List<MessageDto> toMessageDtosOldestFirst(List<MessageState> messagesDesc) {
        List<MessageDto> result = messagesDesc.stream()
                .map(this::toMessageDto)
                .collect(Collectors.toCollection(ArrayList::new));

        Collections.reverse(result);
        return result;
    }
}
