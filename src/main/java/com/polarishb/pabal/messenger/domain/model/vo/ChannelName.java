package com.polarishb.pabal.messenger.domain.model.vo;

public record ChannelName(String value) implements RoomName {

    private static final int MAX_LENGTH = 50;
    private static final int MIN_LENGTH = 1;

    public ChannelName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("채널 이름은 필수입니다");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("채널 이름은 %d자 이상 %d자 이하여야 합니다", MIN_LENGTH, MAX_LENGTH)
            );
        }
        if (!value.matches("^[a-zA-Z0-9가-힣_-]+$")) {
            throw new IllegalArgumentException(
                    "채널 이름은 한글, 영문, 숫자, 언더스코어, 하이픈만 사용 가능합니다"
            );
        }
    }

    @Override
    public String valueOrNull() {
        return value;
    }
}