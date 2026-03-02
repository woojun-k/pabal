package com.polarishb.pabal.messenger.infrastructure.persistence;

import com.polarishb.pabal.messenger.domain.model.entity.DirectChatMapping;
import com.polarishb.pabal.messenger.domain.repository.DirectChatMappingReadRepository;
import com.polarishb.pabal.messenger.domain.repository.DirectChatMappingRepository;
import com.polarishb.pabal.messenger.domain.repository.DirectChatMappingWriteRepository;
import com.polarishb.pabal.messenger.domain.repository.result.DirectChatMappingResult;
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
    public DirectChatMappingResult save(DirectChatMapping mapping) {
        return writeRepository.save(mapping);
    }

    @Override
    public void flush() {
        writeRepository.flush();
    };

    @Override
    public Optional<DirectChatMapping> findByTenantIdAndUserIds(UUID tenantId, UUID userId1, UUID userId2) {
        return readRepository.findByTenantIdAndUserIds(tenantId, userId1, userId2);
    }
}
