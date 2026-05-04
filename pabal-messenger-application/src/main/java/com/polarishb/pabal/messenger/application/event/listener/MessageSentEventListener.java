package com.polarishb.pabal.messenger.application.event.listener;

import com.polarishb.pabal.messenger.application.port.out.realtime.ChatRealtimePort;
import com.polarishb.pabal.messenger.contract.realtime.MessageSentRealtimePayload;
import com.polarishb.pabal.messenger.contract.realtime.RoomEventEnvelope;
import com.polarishb.pabal.messenger.contract.realtime.RoomEventType;
import com.polarishb.pabal.messenger.domain.event.MessageSentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageSentEventListener {

    private final ChatRealtimePort chatRealtimePort;

    @EventListener
    public void handle(MessageSentEvent event) {
        MessageSentRealtimePayload payload = new MessageSentRealtimePayload(
                event.messageId(),
                event.chatRoomId(),
                event.sequence(),
                event.senderId(),
                event.clientMessageId(),
                event.content(),
                event.occurredAt()
        );

        chatRealtimePort.publishRoomEvent(
                RoomEventEnvelope.of(
                        RoomEventType.MESSAGE_SENT,
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
