package com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity;

import com.polarishb.pabal.common.persistence.entity.base.DeletableEntity;
import com.polarishb.pabal.common.persistence.jpa.UuidV7Generated;
import com.polarishb.pabal.messenger.domain.model.entity.Message;
import com.polarishb.pabal.messenger.domain.model.type.MessageStatus;
import com.polarishb.pabal.messenger.domain.model.type.MessageType;
import com.polarishb.pabal.messenger.domain.model.vo.MessageContent;
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

    public static MessageEntity from(Message message) {
        MessageEntity entity = new MessageEntity();
        entity.id = message.getId();
        entity.tenantId = message.getTenantId();
        entity.chatRoomId = message.getChatRoomId();
        entity.senderId = message.getSenderId();
        entity.clientMessageId = message.getClientMessageId();
        entity.type = message.getType();
        entity.content = message.getContent().value();
        entity.status = message.getStatus();
        entity.replyToMessageId = message.getReplyToMessageId();
        return entity;
    }

    public Message toDomain() {
        return Message.reconstitute(
                this.id,
                this.tenantId,
                this.chatRoomId,
                this.senderId,
                this.clientMessageId,
                this.type,
                new MessageContent(this.content),
                this.status,
                this.replyToMessageId,
                this.getCreatedAt(),
                this.getUpdatedAt(),
                this.getDeletedAt()
        );
    }
}
