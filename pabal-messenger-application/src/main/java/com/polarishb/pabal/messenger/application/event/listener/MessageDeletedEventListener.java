package com.polarishb.pabal.messenger.application.event.listener;

import com.polarishb.pabal.messenger.application.port.out.time.ClockPort;
import com.polarishb.pabal.messenger.application.port.out.realtime.ChatRealtimePort;
import com.polarishb.pabal.messenger.contract.persistence.message.PersistedMessage;
import com.polarishb.pabal.messenger.contract.realtime.MessageDeletedRealtimePayload;
import com.polarishb.pabal.messenger.contract.realtime.RoomEventEnvelope;
import com.polarishb.pabal.messenger.contract.realtime.RoomEventType;
import com.polarishb.pabal.messenger.domain.event.MessageDeletedEvent;
import com.polarishb.pabal.messenger.domain.exception.MessageNotFoundException;
import com.polarishb.pabal.messenger.application.port.out.persistence.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageDeletedEventListener {

    private final MessageRepository messageRepository;
    private final ChatRealtimePort chatRealtimePort;
    private final ClockPort clockPort;

    @EventListener
    public void handle(MessageDeletedEvent event) {
        PersistedMessage persisted = messageRepository.findByTenantIdAndId(
                event.tenantId(),
                event.messageId()
        ).orElseThrow(() -> new MessageNotFoundException(event.messageId()));

        MessageDeletedRealtimePayload payload = new MessageDeletedRealtimePayload(
                persisted.state().id(),
                persisted.state().chatRoomId(),
                persisted.state().deletedAt()
        );

        chatRealtimePort.publishRoomEvent(
                event.tenantId(),
                event.chatRoomId(),
                RoomEventEnvelope.of(RoomEventType.MESSAGE_DELETED, payload, clockPort.now())
        );
    }
}
