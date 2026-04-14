package com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity;

import com.polarishb.pabal.common.persistence.entity.base.DeletableEntity;
import com.polarishb.pabal.common.persistence.jpa.UuidV7Generated;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.ChatRoomMemberState;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_room_member",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chat_room_member", columnNames = {"chat_room_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_member_user_room", columnList = "user_id, chat_room_id"),
                @Index(name = "idx_member_room_left", columnList = "chat_room_id, left_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMemberEntity extends DeletableEntity {

    @Id
    @UuidV7Generated(mode = UuidV7Generated.Mode.MONOTONIC)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID chatRoomId;

    @Column(nullable = false)
    private UUID userId;

    private UUID lastReadMessageId;
    private Long lastReadSequence;
    private Instant lastReadAt;

    @Column(nullable = false)
    private Instant joinedAt;

    private Instant leftAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public static ChatRoomMemberEntity fromNewState(ChatRoomMemberState state) {
        ChatRoomMemberEntity entity = new ChatRoomMemberEntity();
        entity.id = state.id();
        entity.tenantId = state.tenantId();
        entity.chatRoomId = state.chatRoomId();
        entity.userId = state.userId();
        entity.lastReadMessageId = state.lastReadMessageId();
        entity.lastReadSequence = state.lastReadSequence();
        entity.lastReadAt = state.lastReadAt();
        entity.joinedAt = state.joinedAt();
        entity.leftAt = state.leftAt();

        entity.setCreatedAt(state.createdAt());
        entity.setUpdatedAt(state.updatedAt());
        return entity;
    }

    public ChatRoomMemberState toState() {
        return new ChatRoomMemberState(
            this.id,
            this.tenantId,
            this.chatRoomId,
            this.userId,
            this.lastReadMessageId,
            this.lastReadSequence,
            this.lastReadAt,
            this.joinedAt,
            this.leftAt,
            this.getCreatedAt(),
            this.getUpdatedAt(),
            this.version
        );
    }

    public void apply(ChatRoomMemberState state) {
        Long incomingSequence = state.lastReadSequence();
        if (incomingSequence != null && (this.lastReadSequence == null || incomingSequence >= this.lastReadSequence)) {
            this.lastReadMessageId = state.lastReadMessageId();
            this.lastReadSequence = incomingSequence;
            this.lastReadAt = state.lastReadAt();
        }

        this.joinedAt = state.joinedAt();
        this.leftAt = state.leftAt();
        this.setUpdatedAt(state.updatedAt());
    }

}
