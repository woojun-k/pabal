package com.polarishb.pabal.messenger.application.event.listener;

import com.polarishb.pabal.messenger.application.port.out.time.ClockPort;
import com.polarishb.pabal.messenger.application.port.out.realtime.ChatRealtimePort;
import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;
import com.polarishb.pabal.messenger.contract.realtime.MessageEditedRealtimePayload;
import com.polarishb.pabal.messenger.contract.realtime.RoomEventEnvelope;
import com.polarishb.pabal.messenger.contract.realtime.RoomEventType;
import com.polarishb.pabal.messenger.domain.event.MessageEditedEvent;
import com.polarishb.pabal.messenger.domain.exception.MessageNotFoundException;
import com.polarishb.pabal.messenger.domain.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageEditedEventListener {

    private final MessageRepository messageRepository;
    private final ChatRealtimePort chatRealtimePort;
    private final ClockPort clockPort;

    @EventListener
    public void handle(MessageEditedEvent event) {
        PersistedMessage persisted = messageRepository.findByTenantIdAndId(
                event.tenantId(),
                event.messageId()
        ).orElseThrow(() -> new MessageNotFoundException(event.messageId()));

        MessageEditedRealtimePayload payload = new MessageEditedRealtimePayload(
                persisted.state().id(),
                persisted.state().chatRoomId(),
                persisted.state().content(),
                persisted.state().updatedAt()
        );

        chatRealtimePort.publishRoomEvent(
                event.tenantId(),
                event.chatRoomId(),
                RoomEventEnvelope.of(RoomEventType.MESSAGE_EDITED, payload, clockPort.now())
        );
    }
}
