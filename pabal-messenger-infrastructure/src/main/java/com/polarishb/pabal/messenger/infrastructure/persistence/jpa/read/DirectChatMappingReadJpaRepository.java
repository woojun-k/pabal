package com.polarishb.pabal.messenger.infrastructure.persistence.jpa.read;

import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.DirectChatMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DirectChatMappingReadJpaRepository extends JpaRepository<DirectChatMappingEntity, UUID> {
    Optional<DirectChatMappingEntity> findByTenantIdAndUserIdMinAndUserIdMax(UUID tenantId, UUID min, UUID max);
}
