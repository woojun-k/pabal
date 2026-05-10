package com.polarishb.pabal.messenger.application.event.listener;

import com.polarishb.pabal.messenger.application.port.out.realtime.ChatRealtimePort;
import com.polarishb.pabal.messenger.contract.realtime.MemberLeftRealtimePayload;
import com.polarishb.pabal.messenger.contract.realtime.RoomEventEnvelope;
import com.polarishb.pabal.messenger.contract.realtime.RoomEventType;
import com.polarishb.pabal.messenger.contract.realtime.RoomSubscriptionRevokedRealtimePayload;
import com.polarishb.pabal.messenger.domain.event.MemberLeftEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberLeftEventListener {

    private final ChatRealtimePort chatRealtimePort;

    @EventListener
    public void handle(MemberLeftEvent event) {
        RoomSubscriptionRevokedRealtimePayload revocationPayload = new RoomSubscriptionRevokedRealtimePayload(
                event.tenantId(),
                event.chatRoomId(),
                event.leftAt()
        );
        MemberLeftRealtimePayload payload = new MemberLeftRealtimePayload(
                event.userId(),
                event.chatRoomId(),
                event.sequence(),
                event.leftAt()
        );

        chatRealtimePort.publishRoomEvent(
                RoomEventEnvelope.of(
                        RoomEventType.MEMBER_LEFT,
                        event.tenantId(),
                        event.chatRoomId(),
                        event.sequence(),
                        event.aggregateVersion(),
                        event.leftAt(),
                        payload
                )
        );

        chatRealtimePort.publishSubscriptionRevocation(
                event.tenantId(),
                event.userId(),
                revocationPayload
        );
    }
}
