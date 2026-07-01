package com.polarishb.pabal.user.application.port.out.persistence;

import com.polarishb.pabal.user.contract.persistence.PersistedUser;
import com.polarishb.pabal.user.contract.persistence.UserState;
import com.polarishb.pabal.user.domain.model.User;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface UserRepository {

    PersistedUser save(User user);

    Optional<PersistedUser> findById(UUID id);

    Optional<PersistedUser> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<UserState> findStateByTenantIdAndId(UUID tenantId, UUID id);

    boolean existsActiveByTenantIdAndId(UUID tenantId, UUID id);

    Set<UUID> findActiveIdsByTenantIdAndIds(UUID tenantId, Set<UUID> ids);
}
