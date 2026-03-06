package com.polarishb.pabal.messenger.domain.model.vo;

public record OptionalName(String value) implements RoomName {

    private static final int MAX_LENGTH = 50;

    public OptionalName {
        if (value != null) {
            value = value.trim();
            if (value.isEmpty()) {
                value = null;
            } else if (value.length() > MAX_LENGTH) {
                throw new IllegalArgumentException("방 이름은 최대 " + MAX_LENGTH + "자까지 가능합니다");
            }
        }
    }

    @Override
    public String valueOrNull() {
        return value;
    }
}