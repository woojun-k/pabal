package com.polarishb.pabal.messenger.application.event.listener;

import com.polarishb.pabal.messenger.application.port.out.time.ClockPort;
import com.polarishb.pabal.messenger.application.port.out.realtime.ChatRealtimePort;
import com.polarishb.pabal.messenger.contract.realtime.MemberLeftRealtimePayload;
import com.polarishb.pabal.messenger.contract.realtime.RoomEventEnvelope;
import com.polarishb.pabal.messenger.contract.realtime.RoomEventType;
import com.polarishb.pabal.messenger.domain.event.MemberLeftEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberLeftEventListener {

    private final ChatRealtimePort chatRealtimePort;
    private final ClockPort clockPort;

    @EventListener
    public void handle(MemberLeftEvent event) {
        var occurredAt = clockPort.now();
        MemberLeftRealtimePayload payload = new MemberLeftRealtimePayload(
                event.userId(),
                event.chatRoomId(),
                occurredAt
        );

        chatRealtimePort.publishRoomEvent(
                event.tenantId(),
                event.chatRoomId(),
                RoomEventEnvelope.of(RoomEventType.MEMBER_LEFT, payload, occurredAt)
        );
    }
}
