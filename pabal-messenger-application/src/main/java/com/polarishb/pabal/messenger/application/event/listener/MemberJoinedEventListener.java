package com.polarishb.pabal.messenger.application.event.listener;

import com.polarishb.pabal.messenger.application.port.out.realtime.ChatRealtimePort;
import com.polarishb.pabal.messenger.contract.realtime.MemberJoinedRealtimePayload;
import com.polarishb.pabal.messenger.contract.realtime.RoomEventEnvelope;
import com.polarishb.pabal.messenger.contract.realtime.RoomEventType;
import com.polarishb.pabal.messenger.domain.event.MemberJoinedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberJoinedEventListener {

    private final ChatRealtimePort chatRealtimePort;

    @EventListener
    public void handle(MemberJoinedEvent event) {
        MemberJoinedRealtimePayload payload = new MemberJoinedRealtimePayload(
                event.userId(),
                event.chatRoomId(),
                event.sequence(),
                event.joinedAt()
        );

        chatRealtimePort.publishRoomEvent(
                RoomEventEnvelope.of(
                        RoomEventType.MEMBER_JOINED,
                        event.tenantId(),
                        event.chatRoomId(),
                        event.sequence(),
                        event.aggregateVersion(),
                        event.joinedAt(),
                        payload
                )
        );
    }
}
