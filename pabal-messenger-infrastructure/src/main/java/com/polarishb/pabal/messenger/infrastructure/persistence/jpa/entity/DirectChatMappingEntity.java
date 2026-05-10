package com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity;

import com.polarishb.pabal.common.persistence.entity.base.UpdatableEntity;
import com.polarishb.pabal.common.persistence.jpa.UuidV7Generated;
import com.polarishb.pabal.messenger.contract.persistence.directchatmapping.DirectChatMappingState;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "direct_chat_mapping")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DirectChatMappingEntity extends UpdatableEntity {

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
        
        entity.setCreatedAt(state.createdAt());
        entity.setUpdatedAt(state.updatedAt());
        return entity;
    }

    public DirectChatMappingState toState() {
        return new DirectChatMappingState(
                this.id,
                this.tenantId,
                this.chatRoomId,
                this.userIdMin,
                this.userIdMax,
                this.getCreatedAt(),
                this.getUpdatedAt(),
                this.version
        );
    }

    public void apply(DirectChatMappingState state) {
        this.chatRoomId = state.chatRoomId();
        this.setUpdatedAt(state.updatedAt());
    }
}
