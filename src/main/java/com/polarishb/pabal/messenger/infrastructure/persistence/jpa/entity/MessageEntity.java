package com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity;

import com.polarishb.pabal.common.persistence.entity.base.DeletableEntity;
import com.polarishb.pabal.common.persistence.jpa.UuidV7Generated;
import com.polarishb.pabal.messenger.contract.persistence.message.MessageState;
import com.polarishb.pabal.messenger.domain.model.type.MessageStatus;
import com.polarishb.pabal.messenger.domain.model.type.MessageType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "message",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_message_client_id", columnNames = {"chat_room_id", "sender_id", "client_message_id"}),
                @UniqueConstraint(name = "uk_message_room_sequence", columnNames = {"chat_room_id", "sequence"})
        },
        indexes = {
                @Index(name = "idx_message_chat_room_created", columnList = "chat_room_id, created_at, id"),
                @Index(name = "idx_message_chat_room_sequence", columnList = "chat_room_id, sequence")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MessageEntity extends DeletableEntity {

    @Id
    @UuidV7Generated(mode = UuidV7Generated.Mode.MONOTONIC)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID chatRoomId;

    @Column(nullable = false)
    private UUID senderId;

    @Column(nullable = false)
    private UUID clientMessageId;

    @Column(nullable = false)
    private Long sequence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;

    @Column(nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status;

    private UUID replyToMessageId;

    @Version
    @Column(nullable = false)
    private Long version;

    public static MessageEntity fromNewState(MessageState state) {
        MessageEntity entity = new MessageEntity();
        entity.applyNewState(state);
        return entity;
    }

    private void applyNewState(MessageState state) {
        Objects.requireNonNull(state);
        this.id = state.id();
        this.tenantId = state.tenantId();
        this.chatRoomId = state.chatRoomId();
        this.senderId = state.senderId();
        this.clientMessageId = state.clientMessageId();
        apply(state);
        setCreatedAt(state.createdAt());
    }

    public MessageState toState() {
        return new MessageState(
                this.id,
                this.tenantId,
                this.chatRoomId,
                this.senderId,
                this.clientMessageId,
                this.sequence,
                this.type,
                this.content,
                this.status,
                this.replyToMessageId,
                this.getCreatedAt(),
                this.getUpdatedAt(),
                this.getDeletedAt(),
                this.version
        );
    }

    public void apply(MessageState state) {
        Objects.requireNonNull(state);
        this.sequence = state.sequence();
        this.type = state.type();
        this.content = state.content();
        this.status = state.status();
        this.replyToMessageId = state.replyToMessageId();
        setUpdatedAt(state.updatedAt());
        setDeletedAt(state.deletedAt());
    }
}
