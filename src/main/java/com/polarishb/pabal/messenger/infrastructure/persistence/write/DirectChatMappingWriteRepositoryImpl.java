package com.polarishb.pabal.messenger.infrastructure.persistence.write;

import com.polarishb.pabal.messenger.domain.exception.DuplicateDirectChatMappingException;
import com.polarishb.pabal.messenger.domain.model.entity.DirectChatMapping;
import com.polarishb.pabal.messenger.domain.repository.DirectChatMappingWriteRepository;
import com.polarishb.pabal.messenger.domain.repository.result.DirectChatMappingResult;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.DirectChatMappingEntity;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.write.DirectChatMappingWriteJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class DirectChatMappingWriteRepositoryImpl
        implements DirectChatMappingWriteRepository {

    private final DirectChatMappingWriteJpaRepository jpaRepository;

    @Override
    public DirectChatMappingResult save(DirectChatMapping mapping) {
        DirectChatMappingEntity entity = DirectChatMappingEntity.from(mapping);
        DirectChatMappingEntity saved = jpaRepository.save(entity);
        return new DirectChatMappingResult(saved.getChatRoomId());
    }

    @Override
    public void flush() {
        try {
            jpaRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateDirectChatMappingException();
        }
    }
}