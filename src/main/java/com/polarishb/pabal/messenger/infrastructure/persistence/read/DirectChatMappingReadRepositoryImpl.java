package com.polarishb.pabal.messenger.infrastructure.persistence.read;

import com.polarishb.pabal.messenger.domain.model.entity.DirectChatMapping;
import com.polarishb.pabal.messenger.domain.repository.DirectChatMappingReadRepository;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.DirectChatMappingEntity;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.read.DirectChatMappingReadJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DirectChatMappingReadRepositoryImpl implements DirectChatMappingReadRepository {

    private final DirectChatMappingReadJpaRepository jpaRepository;

    @Override
    public Optional<DirectChatMapping> findByTenantIdAndUserIds(UUID tenantId, UUID userId1, UUID userId2) {
        UUID min = userId1.compareTo(userId2) < 0 ? userId1 : userId2;
        UUID max = userId1.compareTo(userId2) < 0 ? userId2 : userId1;

        return jpaRepository.findByTenantIdAndUserIdMinAndUserIdMax(tenantId, min, max)
                .map(DirectChatMappingEntity::toDomain);
    }
}
