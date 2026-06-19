package com.polarishb.pabal.user.application.service;

import com.polarishb.pabal.common.contract.UserContract;
import com.polarishb.pabal.common.contract.dto.UserInfo;
import com.polarishb.pabal.user.application.port.out.persistence.UserRepository;
import com.polarishb.pabal.user.contract.persistence.PersistedUser;
import com.polarishb.pabal.user.domain.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserContractService implements UserContract {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean existsUserInTenant(UUID userId, UUID tenantId) {
        return userRepository.existsActiveByTenantIdAndId(tenantId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> findActiveUserIdsInTenant(UUID tenantId, Set<UUID> userIds) {
        return userRepository.findActiveIdsByTenantIdAndIds(tenantId, userIds);
    }

    @Override
    @Transactional(readOnly = true)
    public UserInfo getUserInfo(UUID userId) {
        PersistedUser persistedUser = userRepository.findById(userId)
                .filter(user -> user.user().isActive())
                .orElseThrow(() -> new UserNotFoundException(userId));

        return new UserInfo(
                persistedUser.state().id(),
                persistedUser.state().name(),
                persistedUser.state().tenantId()
        );
    }
}
