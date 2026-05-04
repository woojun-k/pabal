package com.polarishb.pabal.messenger.domain.model;

import com.polarishb.pabal.messenger.domain.exception.InvalidDirectChatParticipantsException;
import com.polarishb.pabal.messenger.domain.exception.code.MessengerErrorCode;
import com.polarishb.pabal.messenger.domain.model.snapshot.DirectChatMappingSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class DirectChatMappingTest {

    @Test
    void create_orders_participant_ids() {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID higherUserId = UUID.fromString("00000000-0000-0000-0000-0000000000ff");
        UUID lowerUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Instant createdAt = Instant.parse("2026-04-02T12:00:00Z");

        DirectChatMapping mapping = DirectChatMapping.create(
                tenantId,
                chatRoomId,
                higherUserId,
                lowerUserId,
                createdAt
        );

        assertThat(mapping.getUserIdMin()).isEqualTo(lowerUserId);
        assertThat(mapping.getUserIdMax()).isEqualTo(higherUserId);
    }

    @Test
    void create_orders_participant_ids_by_database_uuid_order_not_java_signed_order() {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID javaSignedLowerButDatabaseHigher = UUID.fromString("80000000-0000-0000-0000-000000000000");
        UUID databaseLower = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Instant createdAt = Instant.parse("2026-04-02T12:00:00Z");

        assertThat(javaSignedLowerButDatabaseHigher.compareTo(databaseLower)).isNegative();

        DirectChatMapping mapping = DirectChatMapping.create(
                tenantId,
                chatRoomId,
                javaSignedLowerButDatabaseHigher,
                databaseLower,
                createdAt
        );

        assertThat(mapping.getUserIdMin()).isEqualTo(databaseLower);
        assertThat(mapping.getUserIdMax()).isEqualTo(javaSignedLowerButDatabaseHigher);
    }

    @Test
    void snapshot_round_trip_preserves_mapping_state() {
        DirectChatMappingSnapshot snapshot = new DirectChatMappingSnapshot(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("00000000-0000-0000-0000-0000000000ff"),
                Instant.parse("2026-04-02T12:00:00Z"),
                Instant.parse("2026-04-02T12:01:00Z")
        );

        DirectChatMapping mapping = DirectChatMapping.reconstitute(snapshot);

        assertThat(mapping.snapshot()).isEqualTo(snapshot);
    }

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
