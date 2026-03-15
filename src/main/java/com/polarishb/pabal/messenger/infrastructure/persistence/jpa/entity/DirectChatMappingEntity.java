package com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity;

import com.polarishb.pabal.common.persistence.entity.base.BaseEntity;
import com.polarishb.pabal.common.persistence.jpa.UuidV7Generated;
import com.polarishb.pabal.messenger.contract.persistence.directchatmapping.DirectChatMappingState;
import jakarta.persistence.*;
import lombok.AccessLevel;
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
public class DirectChatMappingEntity extends BaseEntity {

    @Id
    @UuidV7Generated(mode = UuidV7Generated.Mode.MONOTONIC)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID chatRoomId;

    @Column(nullable = false)
    private UUID userIdMin;

    @Column(nullable = false)
    private UUID userIdMax;

    @Version
    @Column(nullable = false)
    private Long version;

    public static DirectChatMappingEntity fromNewState(DirectChatMappingState state) {
        DirectChatMappingEntity entity = new DirectChatMappingEntity();
        entity.id = state.id();
        entity.tenantId = state.tenantId();
        entity.chatRoomId = state.chatRoomId();
        entity.userIdMin = state.userIdMin();
        entity.userIdMax = state.userIdMax();
        return entity;
    }

    public DirectChatMappingState toState() {
        return new DirectChatMappingState(
                this.id,
                this.tenantId,
                this.chatRoomId,
                this.userIdMin,
                this.userIdMax,
                this.version
        );
    }

    public void apply(DirectChatMappingState state) {
        // Mapping is immutable in terms of user IDs, but we might update chatRoomId if needed (rare)
        this.chatRoomId = state.chatRoomId();
    }
}