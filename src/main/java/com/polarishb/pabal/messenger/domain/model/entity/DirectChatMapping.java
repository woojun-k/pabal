package com.polarishb.pabal.messenger.domain.model.entity;

import com.polarishb.pabal.common.persistence.jpa.UuidV7Generated;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "direct_chat_mapping",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenantId", "userIdMin", "userIdMax"})
    },
    indexes = {
        @Index(name = "idx_chat_rooms", columnList = "tenantId,chatRoomId")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DirectChatMapping {

    @Id
    @UuidV7Generated(mode = UuidV7Generated.Mode.MONOTONIC)
    private UUID uuid;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID chatRoomId;

    @Column(nullable = false)
    private UUID userIdMin;

    @Column(nullable = false)
    private UUID userIdMax;

    @Builder
    private DirectChatMapping(UUID tenantId, UUID chatRoomId, UUID userId1, UUID userId2) {
        this.tenantId = tenantId;
        this.chatRoomId = chatRoomId;

        int comparison = userId1.compareTo(userId2);
        this.userIdMin = comparison < 0 ? userId1 : userId2;
        this.userIdMax = comparison < 0 ? userId2 : userId1;
    }

    public static DirectChatMapping create(UUID tenantId, UUID chatRoomId, UUID userId1, UUID userId2) {
        return DirectChatMapping.builder()
                .tenantId(tenantId)
                .chatRoomId(chatRoomId)
                .userId1(userId1)
                .userId2(userId2)
                .build();
    }
}
