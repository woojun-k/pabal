package com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity;

import com.polarishb.pabal.common.persistence.entity.base.BaseEntity;
import com.polarishb.pabal.common.persistence.jpa.UuidV7Generated;
import com.polarishb.pabal.messenger.domain.model.entity.DirectChatMapping;
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

    public static DirectChatMappingEntity from(DirectChatMapping mapping) {
        DirectChatMappingEntity entity = new DirectChatMappingEntity();
        entity.id = mapping.getId();
        entity.tenantId = mapping.getTenantId();
        entity.chatRoomId = mapping.getChatRoomId();
        entity.userIdMin = mapping.getUserIdMin();
        entity.userIdMax = mapping.getUserIdMax();
        return entity;
    }

    public DirectChatMapping toDomain() {
        return DirectChatMapping.reconstitute(
                this.id,
                this.tenantId,
                this.chatRoomId,
                this.userIdMin,
                this.userIdMax
        );
    }
}