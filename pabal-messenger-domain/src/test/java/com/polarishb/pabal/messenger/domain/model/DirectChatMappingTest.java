package com.polarishb.pabal.messenger.domain.model;

import com.polarishb.pabal.messenger.domain.exception.InvalidDirectChatParticipantsException;
import com.polarishb.pabal.messenger.domain.exception.code.MessengerErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class DirectChatMappingTest {

    @Test
    void create_rejects_same_user_ids() {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-02T12:00:00Z");

        assertThatExceptionOfType(InvalidDirectChatParticipantsException.class)
                .isThrownBy(() -> DirectChatMapping.create(tenantId, chatRoomId, userId, userId, createdAt))
                .satisfies(exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(MessengerErrorCode.INVALID_DIRECT_CHAT_PARTICIPANTS);
                    assertThat(exception.getPayload())
                            .containsEntry("requesterId", userId)
                            .containsEntry("participantId", userId);
                });
    }
}
