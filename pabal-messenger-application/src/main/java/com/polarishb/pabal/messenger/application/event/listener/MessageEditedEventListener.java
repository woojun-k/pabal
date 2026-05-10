package com.polarishb.pabal.messenger.application.event.listener;

import com.polarishb.pabal.messenger.application.port.out.realtime.ChatRealtimePort;
import com.polarishb.pabal.messenger.contract.realtime.MessageEditedRealtimePayload;
import com.polarishb.pabal.messenger.contract.realtime.RoomEventEnvelope;
import com.polarishb.pabal.messenger.contract.realtime.RoomEventType;
import com.polarishb.pabal.messenger.domain.event.MessageEditedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageEditedEventListener {

    private final ChatRealtimePort chatRealtimePort;

    @EventListener
    public void handle(MessageEditedEvent event) {
        MessageEditedRealtimePayload payload = new MessageEditedRealtimePayload(
                event.messageId(),
                event.chatRoomId(),
                event.sequence(),
                event.content(),
                event.occurredAt()
        );

        chatRealtimePort.publishRoomEvent(
                RoomEventEnvelope.of(
                        RoomEventType.MESSAGE_EDITED,
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
