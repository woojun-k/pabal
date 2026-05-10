package com.polarishb.pabal.messenger.application.event.listener;

import com.polarishb.pabal.messenger.application.port.out.realtime.ChatRealtimePort;
import com.polarishb.pabal.messenger.contract.realtime.MessageReadRealtimePayload;
import com.polarishb.pabal.messenger.contract.realtime.RoomEventEnvelope;
import com.polarishb.pabal.messenger.contract.realtime.RoomEventType;
import com.polarishb.pabal.messenger.domain.event.MessageReadEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageReadEventListener {

    private final ChatRealtimePort chatRealtimePort;

    @EventListener
    public void handle(MessageReadEvent event) {
        MessageReadRealtimePayload payload = new MessageReadRealtimePayload(
                event.userId(),
                event.chatRoomId(),
                event.lastReadMessageId(),
                event.sequence(),
                event.readAt()
        );

        chatRealtimePort.publishRoomEvent(
                RoomEventEnvelope.of(
                        RoomEventType.MESSAGE_READ,
                        event.tenantId(),
                        event.chatRoomId(),
                        event.sequence(),
                        event.aggregateVersion(),
                        event.readAt(),
                        payload
                )
        );
    }
}
