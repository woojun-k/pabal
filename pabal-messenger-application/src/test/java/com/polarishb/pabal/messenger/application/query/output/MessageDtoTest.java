package com.polarishb.pabal.messenger.application.query.output;

import com.polarishb.pabal.messenger.domain.model.type.MessageStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MessageDtoTest {

    @Test
    void content_masks_deleted_message_content_even_when_snapshot_contains_original_text() {
        MessageDto dto = messageDto("sensitive original", MessageStatus.DELETED);

        assertThat(dto.content()).isEqualTo("[deleted]");
        assertThat(dto.status()).isEqualTo("DELETED");
    }

    @Test
    void content_returns_original_text_for_active_message() {
        MessageDto dto = messageDto("hello", MessageStatus.ACTIVE);

        assertThat(dto.content()).isEqualTo("hello");
    }

    private MessageDto messageDto(String content, MessageStatus status) {
        Instant createdAt = Instant.parse("2026-04-02T12:00:00Z");
        Instant deletedAt = status == MessageStatus.DELETED ? createdAt.plusSeconds(60) : null;
        return new MessageDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                1L,
                content,
                status.name(),
                null,
                createdAt,
                deletedAt == null ? createdAt : deletedAt,
                deletedAt
        );
    }
}
