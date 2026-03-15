package com.polarishb.pabal.messenger.infrastructure.persistence.jpa.write;

import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MessageWriteJpaRepository extends JpaRepository<MessageEntity, UUID> {

    Optional<MessageEntity> findByTenantIdAndId(UUID tenantId, UUID id);
}