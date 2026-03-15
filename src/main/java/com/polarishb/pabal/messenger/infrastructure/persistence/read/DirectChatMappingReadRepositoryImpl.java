package com.polarishb.pabal.messenger.infrastructure.persistence.read;

import com.polarishb.pabal.messenger.contract.persistence.directchatmapping.DirectChatMappingPersistenceMapper;
import com.polarishb.pabal.messenger.contract.persistence.directchatmapping.PersistedDirectChatMapping;
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
    public Optional<PersistedDirectChatMapping> findByTenantIdAndUserIds(UUID tenantId, UUID userId1, UUID userId2) {
        int comparison = userId1.compareTo(userId2);
        UUID userIdMin = comparison < 0 ? userId1 : userId2;
        UUID userIdMax = comparison < 0 ? userId2 : userId1;

        return jpaRepository.findByTenantIdAndUserIdMinAndUserIdMax(tenantId, userIdMin, userIdMax)
                .map(DirectChatMappingEntity::toState)
                .map(DirectChatMappingPersistenceMapper::toPersisted);
    }
}
