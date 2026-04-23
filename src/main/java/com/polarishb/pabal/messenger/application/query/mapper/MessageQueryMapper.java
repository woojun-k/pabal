package com.polarishb.pabal.messenger.application.query.mapper;

import com.polarishb.pabal.messenger.application.query.output.MessageDto;
import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MessageQueryMapper {

    public MessageDto toMessageDto(PersistedMessage message) {
        return new MessageDto(message.state().snapshot());
    }

    public List<MessageDto> toMessageDtosOldestFirst(List<PersistedMessage> messagesDesc) {
        List<MessageDto> result = messagesDesc.stream()
                .map(this::toMessageDto)
                .collect(Collectors.toCollection(ArrayList::new));

        Collections.reverse(result);
        return result;
    }
}
