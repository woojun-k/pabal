package com.polarishb.pabal.messenger.domain.model;

import com.polarishb.pabal.messenger.domain.exception.MessageAlreadyDeletedException;
import com.polarishb.pabal.messenger.domain.model.snapshot.MessageSnapshot;
import com.polarishb.pabal.messenger.domain.model.type.MessageStatus;
import com.polarishb.pabal.messenger.domain.model.type.MessageType;
import com.polarishb.pabal.messenger.domain.model.vo.MessageContent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageTest {

    @Test
    void create_builds_active_user_message_with_application_time() {
        UUID tenantId = UUID.randomUUID();
        UUID chatRoomId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID clientMessageId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-02T12:00:00Z");

        Message message = Message.create(
                tenantId,
                chatRoomId,
                senderId,
                clientMessageId,
                "hello",
                createdAt
        );

        assertThat(message.getId()).isNull();
        assertThat(message.getTenantId()).isEqualTo(tenantId);
        assertThat(message.getChatRoomId()).isEqualTo(chatRoomId);
        assertThat(message.getSenderId()).isEqualTo(senderId);
        assertThat(message.getClientMessageId()).isEqualTo(clientMessageId);
        assertThat(message.getSequence()).isNull();
        assertThat(message.getType()).isEqualTo(MessageType.USER);
        assertThat(message.getContent().value()).isEqualTo("hello");
        assertThat(message.getStatus()).isEqualTo(MessageStatus.ACTIVE);
        assertThat(message.getReplyToMessageId()).isNull();
        assertThat(message.getCreatedAt()).isEqualTo(createdAt);
        assertThat(message.getUpdatedAt()).isEqualTo(createdAt);
        assertThat(message.getDeletedAt()).isNull();
    }

    @Test
    void createReply_records_reply_target() {
        UUID replyToMessageId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-02T12:00:00Z");

        Message message = Message.createReply(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                replyToMessageId,
                "reply",
                createdAt
        );

        assertThat(message.getReplyToMessageId()).isEqualTo(replyToMessageId);
        assertThat(message.getStatus()).isEqualTo(MessageStatus.ACTIVE);
    }

    @Test
    void snapshot_round_trip_preserves_message_state() {
        MessageSnapshot snapshot = new MessageSnapshot(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                10L,
                MessageType.USER,
                new MessageContent("edited"),
                MessageStatus.EDITED,
                UUID.randomUUID(),
                Instant.parse("2026-04-02T12:00:00Z"),
                Instant.parse("2026-04-02T12:01:00Z"),
                null
        );

        Message message = Message.reconstitute(snapshot);

        assertThat(message.snapshot()).isEqualTo(snapshot);
    }

    @Test
    void assignSequence_keeps_highest_sequence() {
        Instant createdAt = Instant.parse("2026-04-02T12:00:00Z");
        Message message = Message.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "hello",
                createdAt
        );

        message.assignSequence(10L);
        message.assignSequence(9L);

        assertThat(message.getSequence()).isEqualTo(10L);
    }

    @Test
    void edit_updates_content_status_and_updatedAt() {
        Instant createdAt = Instant.parse("2026-04-02T12:00:00Z");
        Instant editedAt = createdAt.plusSeconds(60);
        Message message = Message.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "before",
                createdAt
        );

        message.edit("after", editedAt);

        assertThat(message.getContent().value()).isEqualTo("after");
        assertThat(message.getStatus()).isEqualTo(MessageStatus.EDITED);
        assertThat(message.getUpdatedAt()).isEqualTo(editedAt);
    }

    @Test
    void edit_rejects_deleted_message() {
        Instant createdAt = Instant.parse("2026-04-02T12:00:00Z");
        Message message = Message.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "before",
                createdAt
        );
        message.delete(createdAt.plusSeconds(30));

        assertThatThrownBy(() -> message.edit("after", createdAt.plusSeconds(60)))
                .isInstanceOf(MessageAlreadyDeletedException.class);
    }

    @Test
    void delete_marks_deleted_and_rejects_second_delete() {
        Instant createdAt = Instant.parse("2026-04-02T12:00:00Z");
        Instant deletedAt = createdAt.plusSeconds(30);
        Message message = Message.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "hello",
                createdAt
        );

        message.delete(deletedAt);

        assertThat(message.getStatus()).isEqualTo(MessageStatus.DELETED);
        assertThat(message.getUpdatedAt()).isEqualTo(deletedAt);
        assertThat(message.getDeletedAt()).isEqualTo(deletedAt);
        assertThatThrownBy(() -> message.delete(deletedAt.plusSeconds(30)))
                .isInstanceOf(MessageAlreadyDeletedException.class);
    }
}
