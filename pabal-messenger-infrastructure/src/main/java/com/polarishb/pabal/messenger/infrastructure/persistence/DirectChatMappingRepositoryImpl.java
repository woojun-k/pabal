package com.polarishb.pabal.messenger.infrastructure.persistence;

import com.polarishb.pabal.messenger.contract.persistence.directchatmapping.PersistedDirectChatMapping;
import com.polarishb.pabal.messenger.application.port.out.persistence.DirectChatMappingReadRepository;
import com.polarishb.pabal.messenger.application.port.out.persistence.DirectChatMappingRepository;
import com.polarishb.pabal.messenger.application.port.out.persistence.DirectChatMappingWriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DirectChatMappingRepositoryImpl implements DirectChatMappingRepository {

    private final DirectChatMappingWriteRepository writeRepository;
    private final DirectChatMappingReadRepository readRepository;

    @Override
    public PersistedDirectChatMapping append(PersistedDirectChatMapping mapping) {
        return writeRepository.append(mapping);
    }

    @Override
    public PersistedDirectChatMapping update(PersistedDirectChatMapping mapping) {
        return writeRepository.update(mapping);
    }

    @Override
    public void flush() {
        writeRepository.flush();
    }

    @Override
    public Optional<PersistedDirectChatMapping> findByTenantIdAndUserIds(UUID tenantId, UUID userId1, UUID userId2) {
        return readRepository.findByTenantIdAndUserIds(tenantId, userId1, userId2);
    }
}
