package com.polarishb.pabal.messenger.domain.model.vo;

import com.polarishb.pabal.messenger.domain.model.type.RoomType;

public sealed interface RoomName permits ChannelName, OptionalName {
    String valueOrNull();

    static RoomName of(RoomType type, String valueOrNull) {
        if (type == null) throw new IllegalArgumentException("type is required");

        return switch (type) {
            case CHANNEL -> new ChannelName(valueOrNull);
            case DIRECT, GROUP -> new OptionalName(valueOrNull);
        };
    }
}