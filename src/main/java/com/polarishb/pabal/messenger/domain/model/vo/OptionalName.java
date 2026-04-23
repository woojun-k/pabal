package com.polarishb.pabal.messenger.domain.model.vo;

import com.polarishb.pabal.common.exception.InvalidInputException;

public record OptionalName(String value) implements RoomName {

    public static final int MAX_LENGTH = 50;

    public OptionalName {
        if (value != null) {
            value = value.trim();
            if (value.length() > MAX_LENGTH) {
                throw new InvalidInputException("방 이름은 최대 " + MAX_LENGTH + "자까지 가능합니다");
            }
        }
    }

    @Override
    public String valueOrNull() {
        return value;
    }
}
