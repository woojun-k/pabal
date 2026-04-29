package com.polarishb.pabal.messenger.infrastructure.persistence.jpa.write;

import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.DirectChatMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DirectChatMappingWriteJpaRepository extends JpaRepository<DirectChatMappingEntity, UUID> {
}
