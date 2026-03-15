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

import java.util.UUID;

@Entity
@Table(name = "message",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"chat_room_id", "sender_id", "client_message_id"})
    },
    indexes = {
        @Index(name = "idx_chat_room_created", columnList = "chatRoomId,createdAt,id")
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
        entity.id = state.id();
        entity.tenantId = state.tenantId();
        entity.chatRoomId = state.chatRoomId();
        entity.senderId = state.senderId();
        entity.clientMessageId = state.clientMessageId();
        entity.type = state.type();
        entity.content = state.content();
        entity.status = state.status();
        entity.replyToMessageId = state.replyToMessageId();
        entity.setCreatedAt(state.createdAt());
        entity.setUpdatedAt(state.updatedAt());
        entity.setDeletedAt(state.deletedAt());
        return entity;
    }

    public MessageState toState() {
        return new MessageState(
                this.id,
                this.tenantId,
                this.chatRoomId,
                this.senderId,
                this.clientMessageId,
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
        this.type = state.type();
        this.content = state.content();
        this.status = state.status();
        this.replyToMessageId = state.replyToMessageId();
        this.setUpdatedAt(state.updatedAt());
        this.setDeletedAt(state.deletedAt());
    }
}
