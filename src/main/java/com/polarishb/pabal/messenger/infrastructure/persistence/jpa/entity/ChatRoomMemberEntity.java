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
                @UniqueConstraint(columnNames = {"chatRoomId", "userId"})
        },
        indexes = {
                @Index(name = "idx_user_chat_room", columnList = "userId,chatRoomId"),
                @Index(name = "idx_active_members", columnList = "chatRoomId,leftAt")
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
        entity.lastReadAt = state.lastReadAt();
        entity.joinedAt = state.joinedAt();
        entity.leftAt = state.leftAt();
        return entity;
    }

    public ChatRoomMemberState toState() {
        return new ChatRoomMemberState(
            this.id,
            this.tenantId,
            this.chatRoomId,
            this.userId,
            this.lastReadMessageId,
            this.lastReadAt,
            this.joinedAt,
            this.leftAt,
            this.version
        );
    }

    public void apply(ChatRoomMemberState state) {
        this.lastReadMessageId = state.lastReadMessageId();
        this.lastReadAt = state.lastReadAt();
        this.joinedAt = state.joinedAt();
        this.leftAt = state.leftAt();
    }

}
