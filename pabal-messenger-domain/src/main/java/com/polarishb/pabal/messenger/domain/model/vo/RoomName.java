package com.polarishb.pabal.messenger.domain.model.vo;

import com.polarishb.pabal.common.exception.InvalidInputException;
import com.polarishb.pabal.messenger.domain.model.type.RoomType;

import java.util.Optional;

public interface RoomName {
    String valueOrNull();

    static RoomName of(RoomType type, String value) {
        if (type == null) throw new InvalidInputException("RoomType is required");
        return switch (type) {
            case CHANNEL -> new ChannelName(value);
            case GROUP, DIRECT -> new OptionalName(value);
        };
    }
}
