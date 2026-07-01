package com.polarishb.pabal.tenant.infrastructure.persistence;

import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRegistrationRepository;
import com.polarishb.pabal.tenant.contract.persistence.PersistedTenantRegistration;
import com.polarishb.pabal.tenant.contract.persistence.TenantRegistrationPersistenceMapper;
import com.polarishb.pabal.tenant.contract.persistence.TenantRegistrationState;
import com.polarishb.pabal.tenant.domain.exception.TenantDomainAlreadyRegisteredException;
import com.polarishb.pabal.tenant.domain.model.type.TenantRegistrationStatus;
import com.polarishb.pabal.tenant.domain.model.vo.TenantDomainName;
import com.polarishb.pabal.tenant.infrastructure.persistence.jpa.TenantRegistrationJpaRepository;
import com.polarishb.pabal.tenant.infrastructure.persistence.jpa.entity.TenantRegistrationEntity;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TenantRegistrationRepositoryImpl implements TenantRegistrationRepository {

    private static final List<TenantRegistrationStatus> OPEN_STATUSES = List.of(
            TenantRegistrationStatus.PENDING_VERIFICATION,
            TenantRegistrationStatus.VERIFIED,
            TenantRegistrationStatus.ACTIVATED
    );

    private final TenantRegistrationJpaRepository jpaRepository;

    @Override
    public PersistedTenantRegistration append(PersistedTenantRegistration registration) {
        TenantRegistrationState state = registration.state();
        try {
            TenantRegistrationEntity saved = jpaRepository.save(TenantRegistrationEntity.fromNewState(state));
            return TenantRegistrationPersistenceMapper.toPersisted(saved.toState());
        } catch (DataIntegrityViolationException e) {
            if (isDomainUniqueViolation(e)) {
                throw new TenantDomainAlreadyRegisteredException(new TenantDomainName(state.domainName()));
            }
            throw e;
        }
    }

    @Override
    public PersistedTenantRegistration update(PersistedTenantRegistration registration) {
        TenantRegistrationState currentState = registration.state();
        TenantRegistrationEntity entity = jpaRepository.findById(currentState.id())
                .orElseThrow(() -> new EntityNotFoundException("TenantRegistration not found"));

        if (!Objects.equals(entity.getVersion(), currentState.version())) {
            throw new ObjectOptimisticLockingFailureException(
                    TenantRegistrationEntity.class,
                    currentState.id()
            );
        }

        TenantRegistrationState nextState = TenantRegistrationPersistenceMapper.toState(
                registration.registration(),
                currentState.version()
        );
        entity.apply(nextState);

        return TenantRegistrationPersistenceMapper.toPersisted(entity.toState());
    }

    @Override
    public Optional<PersistedTenantRegistration> findById(UUID registrationId) {
        return jpaRepository.findById(registrationId)
                .map(TenantRegistrationEntity::toState)
                .map(TenantRegistrationPersistenceMapper::toPersisted);
    }

    @Override
    public Optional<TenantRegistrationState> findStateById(UUID registrationId) {
        return jpaRepository.findById(registrationId)
                .map(TenantRegistrationEntity::toState);
    }

    @Override
    public Optional<PersistedTenantRegistration> findByIdForUpdate(UUID registrationId) {
        return jpaRepository.findByIdForUpdate(registrationId)
                .map(TenantRegistrationEntity::toState)
                .map(TenantRegistrationPersistenceMapper::toPersisted);
    }

    @Override
    public boolean existsOpenByDomainName(TenantDomainName domainName) {
        return jpaRepository.existsByDomainNameAndStatusIn(domainName.value(), OPEN_STATUSES);
    }

    @Override
    public List<UUID> findPendingVerificationIds(Instant now, int limit) {
        return jpaRepository.findPendingVerificationIds(
                TenantRegistrationStatus.PENDING_VERIFICATION,
                now,
                PageRequest.of(0, limit)
        );
    }

    @Override
    public int expirePendingRegistrations(Instant now) {
        return jpaRepository.expirePendingRegistrations(
                TenantRegistrationStatus.PENDING_VERIFICATION,
                TenantRegistrationStatus.EXPIRED,
                now
        );
    }

    private boolean isDomainUniqueViolation(DataIntegrityViolationException cause) {
        Throwable current = cause;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("uq_tenant_registration_domain_open")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
