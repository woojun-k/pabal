package com.polarishb.pabal.tenant.application.port.out.persistence;

import com.polarishb.pabal.tenant.contract.persistence.PersistedTenantRegistration;
import com.polarishb.pabal.tenant.domain.model.vo.TenantDomainName;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantRegistrationRepository {
    PersistedTenantRegistration append(PersistedTenantRegistration registration);
    PersistedTenantRegistration update(PersistedTenantRegistration registration);
    Optional<PersistedTenantRegistration> findById(UUID registrationId);
    Optional<PersistedTenantRegistration> findByIdForUpdate(UUID registrationId);
    boolean existsOpenByDomainName(TenantDomainName domainName);
    List<UUID> findPendingVerificationIds(Instant now, int limit);
    int expirePendingRegistrations(Instant now);
}
