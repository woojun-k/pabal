package com.polarishb.pabal.messenger.infrastructure.persistence.write;

import com.polarishb.pabal.messenger.contract.persistence.directchatmapping.DirectChatMappingPersistenceMapper;
import com.polarishb.pabal.messenger.contract.persistence.directchatmapping.DirectChatMappingState;
import com.polarishb.pabal.messenger.contract.persistence.directchatmapping.PersistedDirectChatMapping;
import com.polarishb.pabal.messenger.domain.exception.DuplicateDirectChatMappingException;
import com.polarishb.pabal.messenger.domain.model.entity.DirectChatMapping;
import com.polarishb.pabal.messenger.domain.repository.DirectChatMappingWriteRepository;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.entity.DirectChatMappingEntity;
import com.polarishb.pabal.messenger.infrastructure.persistence.jpa.write.DirectChatMappingWriteJpaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class DirectChatMappingWriteRepositoryImpl implements DirectChatMappingWriteRepository {

    private final DirectChatMappingWriteJpaRepository jpaRepository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public PersistedDirectChatMapping append(PersistedDirectChatMapping persistedMapping) {
        DirectChatMappingState state = persistedMapping.state();
        try {
            DirectChatMappingEntity saved = jpaRepository.save(DirectChatMappingEntity.fromNewState(state));
            return DirectChatMappingPersistenceMapper.toPersisted(saved.toState());
        } catch (DataIntegrityViolationException e) {
            throw duplicateDirectMapping(e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public PersistedDirectChatMapping update(PersistedDirectChatMapping persistedMapping) {
        DirectChatMappingState currentState = persistedMapping.state();
        DirectChatMapping mapping = persistedMapping.mapping();

        DirectChatMappingEntity entity = jpaRepository.findById(currentState.id())
                .orElseThrow(() -> new EntityNotFoundException("DirectChatMapping not found"));

        if (!Objects.equals(entity.getVersion(), currentState.version())) {
            throw new ObjectOptimisticLockingFailureException(
                    DirectChatMappingEntity.class,
                    currentState.id()
            );
        }

        DirectChatMappingState nextState = DirectChatMappingPersistenceMapper.toState(
                mapping,
                currentState.version()
        );

        entity.apply(nextState);

        return DirectChatMappingPersistenceMapper.toPersisted(entity.toState());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void flush() {
        try {
            jpaRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw duplicateDirectMapping(e);
        }
    }

    private DuplicateDirectChatMappingException duplicateDirectMapping(DataIntegrityViolationException cause) {
        return new DuplicateDirectChatMappingException(cause.getMessage(), cause);
    }
}
