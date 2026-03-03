package com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity;

import com.polarishb.pabal.common.persistence.entity.base.DeletableEntity;
import com.polarishb.pabal.common.persistence.jpa.UuidV7Generated;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoomMember;
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

    public static ChatRoomMemberEntity from(ChatRoomMember chatRoomMember) {
        ChatRoomMemberEntity entity = new ChatRoomMemberEntity();
        entity.id = chatRoomMember.getId();
        entity.tenantId = chatRoomMember.getTenantId();
        entity.chatRoomId = chatRoomMember.getChatRoomId();
        entity.userId = chatRoomMember.getUserId();
        entity.lastReadMessageId = chatRoomMember.getLastReadMessageId();
        entity.lastReadAt = chatRoomMember.getLastReadAt();
        entity.joinedAt = chatRoomMember.getJoinedAt();
        entity.leftAt = chatRoomMember.getLeftAt();
        return entity;
    }

    public ChatRoomMember toDomain() {
        return ChatRoomMember.reconstitute(
            this.id,
            this.tenantId,
            this.chatRoomId,
            this.userId,
            this.lastReadMessageId,
            this.lastReadAt,
            this.joinedAt,
            this.leftAt
        );
    }

}
