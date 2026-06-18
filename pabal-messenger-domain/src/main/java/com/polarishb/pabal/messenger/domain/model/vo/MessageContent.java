package com.polarishb.pabal.messenger.domain.model.vo;

import com.polarishb.pabal.common.exception.InvalidInputException;

public record MessageContent(String value) {

    public static final String DELETED_TOMBSTONE_VALUE = "[deleted]";

    private static final int MAX_LENGTH = 5000;

    public MessageContent {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidInputException("메시지 내용은 필수입니다");
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidInputException(
                    String.format("메시지 내용은 %d자 이하여야 합니다", MAX_LENGTH)
            );
        }
    }

    public static MessageContent deletedTombstone() {
        return new MessageContent(DELETED_TOMBSTONE_VALUE);
    }
}
