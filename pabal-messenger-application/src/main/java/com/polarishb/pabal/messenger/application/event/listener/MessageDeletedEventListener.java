package com.polarishb.pabal.messenger.application.event.listener;

import com.polarishb.pabal.messenger.application.port.out.realtime.ChatRealtimePort;
import com.polarishb.pabal.messenger.contract.realtime.MessageDeletedRealtimePayload;
import com.polarishb.pabal.messenger.contract.realtime.RoomEventEnvelope;
import com.polarishb.pabal.messenger.contract.realtime.RoomEventType;
import com.polarishb.pabal.messenger.domain.event.MessageDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageDeletedEventListener {

    private final ChatRealtimePort chatRealtimePort;

    @EventListener
    public void handle(MessageDeletedEvent event) {
        MessageDeletedRealtimePayload payload = new MessageDeletedRealtimePayload(
                event.messageId(),
                event.chatRoomId(),
                event.sequence(),
                event.occurredAt()
        );

        chatRealtimePort.publishRoomEvent(
                RoomEventEnvelope.of(
                        RoomEventType.MESSAGE_DELETED,
                        event.tenantId(),
                        event.chatRoomId(),
                        event.sequence(),
                        event.aggregateVersion(),
                        event.occurredAt(),
                        payload
                )
        );
    }
}
