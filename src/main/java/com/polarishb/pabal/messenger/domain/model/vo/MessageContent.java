package com.polarishb.pabal.messenger.domain.model.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MessageContent {

    private static final int MAX_LENGTH = 5000;

    @Column(nullable = false)
    private String value;

    public MessageContent(String value) {
        validate(value);
        this.value = value;
    }

    private void validate(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("메시지 내용은 필수입니다");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("메시지는 %d자를 초과할 수 없습니다", MAX_LENGTH)
            );
        }
    }
}
