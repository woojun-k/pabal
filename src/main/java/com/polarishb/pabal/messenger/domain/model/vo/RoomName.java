package com.polarishb.pabal.messenger.domain.model.vo;

public sealed interface RoomName permits ChannelName, OptionalName {
    String valueOrNull();
}