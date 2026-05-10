package com.polarishb.pabal.messenger.application.authorization;

public enum MessengerPermission {

    CHANNEL_CREATE("messenger:channel:create"),
    CHANNEL_DELETE_SCHEDULE_OWN("messenger:channel:delete:schedule:own"),
    CHANNEL_DELETE_SCHEDULE_ANY("messenger:channel:delete:schedule:any"),
    CHANNEL_DELETE_EXECUTE_OWN("messenger:channel:delete:execute:own"),
    CHANNEL_DELETE_EXECUTE_ANY("messenger:channel:delete:execute:any");

    private final String value;

    MessengerPermission(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
