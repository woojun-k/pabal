package com.polarishb.pabal.tenant.application.port.out.persistence;

import com.polarishb.pabal.tenant.contract.persistence.PersistedTenantRegistration;
import com.polarishb.pabal.tenant.contract.persistence.TenantRegistrationState;
import com.polarishb.pabal.tenant.domain.model.vo.TenantDomainName;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantRegistrationRepository {
    PersistedTenantRegistration append(PersistedTenantRegistration registration);
    PersistedTenantRegistration update(PersistedTenantRegistration registration);
    Optional<PersistedTenantRegistration> findById(UUID registrationId);
    Optional<TenantRegistrationState> findStateById(UUID registrationId);
    Optional<PersistedTenantRegistration> findByIdForUpdate(UUID registrationId);
    boolean existsOpenByDomainName(TenantDomainName domainName);
    List<UUID> findPendingVerificationIds(Instant now, int limit);
    int expirePendingRegistrations(Instant now);
    List<UUID> findLapsedDomainVerifiedIds(Instant now, int limit);

    /**
     * ADR-0013 follow-up, grace-window terminal expiry: bulk-expires REVERIFICATION_REQUIRED
     * rows whose {@code activationExpiresAt + graceMs <= now}, mirroring the
     * {@code expirePendingRegistrations} bulk-UPDATE pattern. DOMAIN_VERIFIED rows are
     * never touched by this method - they must lapse through
     * {@link #findLapsedDomainVerifiedIds(Instant, int)} first.
     *
     * @return the number of rows transitioned to EXPIRED
     */
    int expireLapsedReverificationRegistrations(Instant now, long graceMs);
}
