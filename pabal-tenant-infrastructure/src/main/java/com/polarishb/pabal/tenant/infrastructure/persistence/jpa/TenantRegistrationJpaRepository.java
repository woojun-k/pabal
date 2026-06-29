package com.polarishb.pabal.tenant.infrastructure.persistence.jpa;

import com.polarishb.pabal.tenant.domain.model.type.TenantRegistrationStatus;
import com.polarishb.pabal.tenant.infrastructure.persistence.jpa.entity.TenantRegistrationEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantRegistrationJpaRepository extends JpaRepository<TenantRegistrationEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from TenantRegistrationEntity r where r.id = :id")
    Optional<TenantRegistrationEntity> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByDomainNameAndStatusIn(String domainName, Collection<TenantRegistrationStatus> statuses);

    @Query("""
            SELECT r.id
              FROM TenantRegistrationEntity r
             WHERE r.status = :pendingStatus
               AND r.expiresAt > :now
             ORDER BY r.createdAt ASC
            """)
    List<UUID> findPendingVerificationIds(
            @Param("pendingStatus") TenantRegistrationStatus pendingStatus,
            @Param("now") Instant now,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE TenantRegistrationEntity r
               SET r.status = :expiredStatus,
                   r.updatedAt = :now
             WHERE r.status = :pendingStatus
               AND r.expiresAt <= :now
            """)
    int expirePendingRegistrations(
            @Param("pendingStatus") TenantRegistrationStatus pendingStatus,
            @Param("expiredStatus") TenantRegistrationStatus expiredStatus,
            @Param("now") Instant now
    );
}
