package com.polarishb.pabal.messenger.domain.model.vo;

public record OptionalName(String value) implements RoomName {

    private static final int MAX_LENGTH = 50;

    public OptionalName {
        if (value != null) {
            if (value.isBlank()) {
                throw new IllegalArgumentException("방 이름이 공백일 수 없습니다");
            }
            if (value.length() > MAX_LENGTH) {
                throw new IllegalArgumentException("방 이름은 최대 " + MAX_LENGTH + "자까지 가능합니다");
            }
        }
    }

    @Override
    public String valueOrNull() {
        return value;
    }
}