package com.polarishb.pabal.messenger.domain.model.type;

public enum RoomAccessOperation {
    SEND("send"),
    READ("read"),
    SUBSCRIBE("subscribe"),
    JOIN("join");

    private final String value;

    RoomAccessOperation(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}