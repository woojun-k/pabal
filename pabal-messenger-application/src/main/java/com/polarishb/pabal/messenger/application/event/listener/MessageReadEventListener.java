package com.polarishb.pabal.messenger.application.event.listener;

import com.polarishb.pabal.messenger.application.port.out.time.ClockPort;
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
    private final ClockPort clockPort;

    @EventListener
    public void handle(MessageReadEvent event) {
        MessageReadRealtimePayload payload = new MessageReadRealtimePayload(
                event.userId(),
                event.chatRoomId(),
                event.lastReadMessageId(),
                event.readAt()
        );

        chatRealtimePort.publishRoomEvent(
                event.tenantId(),
                event.chatRoomId(),
                RoomEventEnvelope.of(RoomEventType.MESSAGE_READ, payload, clockPort.now())
        );
    }
}
