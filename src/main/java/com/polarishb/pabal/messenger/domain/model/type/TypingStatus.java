package com.polarishb.pabal.messenger.domain.model.type;

import com.polarishb.pabal.common.exception.InvalidInputException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TypingStatus {
    STARTED("STARTED"),
    STOPPED("STOPPED");

    private final String value;

    public static TypingStatus from(String value) {
        for (TypingStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new InvalidInputException("Unknown typing status: " + value);
    }
}
