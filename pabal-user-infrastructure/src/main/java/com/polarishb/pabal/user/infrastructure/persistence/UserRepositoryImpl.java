package com.polarishb.pabal.user.infrastructure.persistence;

import com.polarishb.pabal.user.application.port.out.persistence.UserRepository;
import com.polarishb.pabal.user.contract.persistence.PersistedUser;
import com.polarishb.pabal.user.contract.persistence.UserPersistenceMapper;
import com.polarishb.pabal.user.contract.persistence.UserState;
import com.polarishb.pabal.user.domain.model.User;
import com.polarishb.pabal.user.domain.model.type.UserStatus;
import com.polarishb.pabal.user.infrastructure.persistence.jpa.TenantUserJpaRepository;
import com.polarishb.pabal.user.infrastructure.persistence.jpa.entity.TenantUserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final TenantUserJpaRepository jpaRepository;

    @Override
    public PersistedUser save(User user) {
        UserState state = UserPersistenceMapper.toState(user, null);
        TenantUserEntity saved = jpaRepository.save(TenantUserEntity.fromState(state));
        return UserPersistenceMapper.toPersisted(saved.toState());
    }

    @Override
    public Optional<PersistedUser> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(TenantUserEntity::toState)
                .map(UserPersistenceMapper::toPersisted);
    }

    @Override
    public Optional<PersistedUser> findByTenantIdAndId(UUID tenantId, UUID id) {
        return jpaRepository.findByTenantIdAndId(tenantId, id)
                .map(TenantUserEntity::toState)
                .map(UserPersistenceMapper::toPersisted);
    }

    @Override
    public boolean existsActiveByTenantIdAndId(UUID tenantId, UUID id) {
        return jpaRepository.existsByTenantIdAndIdAndStatus(tenantId, id, UserStatus.ACTIVE);
    }
}
