package com.polarishb.pabal.messenger.contract.realtime;

public sealed interface RoomEventPayload
        permits MemberJoinedRealtimePayload,
        MemberLeftRealtimePayload,
        MessageDeletedRealtimePayload,
        MessageEditedRealtimePayload,
        MessageReadRealtimePayload,
        MessageSentRealtimePayload {
}
