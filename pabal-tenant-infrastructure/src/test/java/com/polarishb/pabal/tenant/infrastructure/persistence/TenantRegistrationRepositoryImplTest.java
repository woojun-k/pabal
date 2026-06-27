package com.polarishb.pabal.tenant.infrastructure.persistence;

import com.polarishb.pabal.support.AbstractTenantPostgresDataJpaTest;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRegistrationRepository;
import com.polarishb.pabal.tenant.contract.persistence.PersistedTenantRegistration;
import com.polarishb.pabal.tenant.contract.persistence.TenantRegistrationPersistenceMapper;
import com.polarishb.pabal.tenant.domain.model.TenantRegistration;
import com.polarishb.pabal.tenant.domain.model.type.TenantRegistrationStatus;
import com.polarishb.pabal.tenant.domain.model.vo.TenantDomainName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TenantRegistrationRepositoryImplTest extends AbstractTenantPostgresDataJpaTest {

    @Autowired
    private TenantRegistrationRepository tenantRegistrationRepository;

    @Test
    void append_and_findById_round_trips_pending_registration() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "Example.COM",
                "abcdefghijklmnopqrstuvwxyzABCDEF",
                now,
                now.plus(java.time.Duration.ofDays(7))
        );

        PersistedTenantRegistration saved = tenantRegistrationRepository.append(
                new PersistedTenantRegistration(
                        registration,
                        TenantRegistrationPersistenceMapper.toState(registration, null)
                )
        );
        Optional<PersistedTenantRegistration> found = tenantRegistrationRepository.findById(saved.state().id());

        assertThat(saved.state().id()).isNotNull();
        assertThat(saved.state().tenantName()).isEqualTo("Acme");
        assertThat(saved.state().domainName()).isEqualTo("example.com");
        assertThat(saved.state().version()).isEqualTo(0L);
        assertThat(found).isPresent();
        assertThat(found.orElseThrow().state().id()).isEqualTo(saved.state().id());
        assertThat(tenantRegistrationRepository.existsOpenByDomainName(new TenantDomainName("example.com"))).isTrue();
    }

    @Test
    void expirePendingRegistrations_marks_expired_pending_rows_as_expired() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                "abcdefghijklmnopqrstuvwxyzABCDEF",
                now.minus(java.time.Duration.ofDays(8)),
                now.minus(java.time.Duration.ofDays(1))
        );

        PersistedTenantRegistration saved = tenantRegistrationRepository.append(
                new PersistedTenantRegistration(
                        registration,
                        TenantRegistrationPersistenceMapper.toState(registration, null)
                )
        );

        int expiredCount = tenantRegistrationRepository.expirePendingRegistrations(now);
        PersistedTenantRegistration found = tenantRegistrationRepository.findById(saved.state().id()).orElseThrow();

        assertThat(expiredCount).isEqualTo(1);
        assertThat(found.state().status()).isEqualTo(TenantRegistrationStatus.EXPIRED);
        assertThat(tenantRegistrationRepository.existsOpenByDomainName(new TenantDomainName("example.com"))).isFalse();
    }

    @Test
    void findPendingVerificationIds_returns_unexpired_pending_rows() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                "abcdefghijklmnopqrstuvwxyzABCDEF",
                now,
                now.plus(java.time.Duration.ofDays(7))
        );
        PersistedTenantRegistration saved = tenantRegistrationRepository.append(
                new PersistedTenantRegistration(
                        registration,
                        TenantRegistrationPersistenceMapper.toState(registration, null)
                )
        );

        assertThat(tenantRegistrationRepository.findPendingVerificationIds(now, 10))
                .containsExactly(saved.state().id());
    }
}
