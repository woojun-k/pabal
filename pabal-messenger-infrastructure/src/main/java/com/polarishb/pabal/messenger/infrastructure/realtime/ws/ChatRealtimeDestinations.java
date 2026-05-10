package com.polarishb.pabal.messenger.infrastructure.realtime.ws;

import java.util.UUID;

public final class ChatRealtimeDestinations {

    private static final String USER_CONTROL_DESTINATION = "/queue/chat.control";
    private static final String USER_CONTROL_SUBSCRIPTION = "/user" + USER_CONTROL_DESTINATION;

    private ChatRealtimeDestinations() {
    }

    public static String roomEventsTopic(UUID tenantId, UUID chatRoomId) {
        return "/topic/tenants/" + tenantId + "/chat-rooms/" + chatRoomId + "/events";
    }

    public static String typingTopic(UUID tenantId, UUID chatRoomId) {
        return "/topic/tenants/" + tenantId + "/chat-rooms/" + chatRoomId + "/typing";
    }

    public static String userControlDestination() {
        return USER_CONTROL_DESTINATION;
    }

    public static String userControlSubscription() {
        return USER_CONTROL_SUBSCRIPTION;
    }
}
