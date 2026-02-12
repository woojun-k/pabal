package com.polarishb.pabal.messenger.domain.exception;

import java.util.UUID;

public class MemberNotActiveException extends RuntimeException {
    public MemberNotActiveException(UUID userId) {
        super(String.format("비활성 상태의 멤버입니다: %s", userId));
    }
}
