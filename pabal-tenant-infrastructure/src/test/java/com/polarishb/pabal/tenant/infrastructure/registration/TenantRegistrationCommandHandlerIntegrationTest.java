package com.polarishb.pabal.tenant.infrastructure.registration;

import com.polarishb.pabal.support.AbstractTenantPostgresDataJpaTest;
import com.polarishb.pabal.tenant.application.command.handler.*;
import com.polarishb.pabal.tenant.application.command.input.*;
import com.polarishb.pabal.tenant.application.command.output.ActivateTenantRegistrationResult;
import com.polarishb.pabal.tenant.application.command.output.PollTenantDomainVerificationsResult;
import com.polarishb.pabal.tenant.application.command.output.ReverifyTenantRegistrationResult;
import com.polarishb.pabal.tenant.application.command.output.TenantRegistrationResult;
import com.polarishb.pabal.tenant.application.command.output.VerifyTenantDomainResult;
import com.polarishb.pabal.tenant.application.port.out.dns.DnsTxtLookupPort;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRegistrationRepository;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRepository;
import com.polarishb.pabal.tenant.application.port.out.time.ClockPort;
import com.polarishb.pabal.tenant.application.port.out.token.TenantVerificationTokenGeneratorPort;
import com.polarishb.pabal.tenant.contract.persistence.PersistedTenant;
import com.polarishb.pabal.tenant.contract.persistence.PersistedTenantRegistration;
import com.polarishb.pabal.tenant.contract.persistence.TenantRegistrationPersistenceMapper;
import com.polarishb.pabal.tenant.domain.exception.TenantDomainAlreadyRegisteredException;
import com.polarishb.pabal.tenant.domain.exception.TenantDomainVerificationFailedException;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationExpiredException;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationNotPendingException;
import com.polarishb.pabal.tenant.domain.model.TenantRegistration;
import com.polarishb.pabal.tenant.domain.model.type.TenantRegistrationStatus;
import com.polarishb.pabal.tenant.domain.model.type.TenantStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import({
        CreateTenantCommandHandler.class,
        RequestTenantRegistrationCommandHandler.class,
        RenewTenantRegistrationTokenCommandHandler.class,
        VerifyTenantDomainCommandHandler.class,
        ActivateTenantRegistrationCommandHandler.class,
        ReverifyTenantRegistrationCommandHandler.class,
        ExpireTenantRegistrationsCommandHandler.class,
        PollTenantDomainVerificationsCommandHandler.class,
        ReverifyLapsedTenantRegistrationsCommandHandler.class,
        TenantDomainVerificationPollScheduler.class,
        TenantRegistrationExpirationScheduler.class,
        TenantRegistrationCommandHandlerIntegrationTest.TestPorts.class
})
@TestPropertySource(properties = {
        "pabal.tenant.registration.verification-window-ms=604800000",
        "pabal.tenant.registration.activation-window-ms=604800000",
        "pabal.tenant.registration.reverification-sweep-delay-ms=600000",
        "pabal.tenant.registration.reverification-grace-ms=604800000"
})
class TenantRegistrationCommandHandlerIntegrationTest extends AbstractTenantPostgresDataJpaTest {

    private static final Duration VERIFICATION_TTL = Duration.ofDays(7);
    private static final String TOKEN_ONE = "abcdefghijklmnopqrstuvwxyzABCDEF";
    private static final String TOKEN_TWO = "ABCDEFabcdefghijklmnopqrstuvwxyz";

    @Autowired
    private CreateTenantCommandHandler createTenantHandler;

    @Autowired
    private RequestTenantRegistrationCommandHandler requestHandler;

    @Autowired
    private RenewTenantRegistrationTokenCommandHandler renewHandler;

    @Autowired
    private VerifyTenantDomainCommandHandler verifyHandler;

    @Autowired
    private ActivateTenantRegistrationCommandHandler activateHandler;

    @Autowired
    private ReverifyTenantRegistrationCommandHandler reverifyRegistrationHandler;

    @Autowired
    private ExpireTenantRegistrationsCommandHandler expireHandler;

    @Autowired
    private PollTenantDomainVerificationsCommandHandler pollHandler;

    @Autowired
    private ReverifyLapsedTenantRegistrationsCommandHandler reverifyHandler;

    @Autowired
    private TenantDomainVerificationPollScheduler verificationPollScheduler;

    @Autowired
    private TenantRegistrationExpirationScheduler expirationScheduler;

    @Autowired
    private TenantRegistrationRepository tenantRegistrationRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private MutableClockPort clockPort;

    @Autowired
    private QueueTokenGeneratorPort tokenGeneratorPort;

    @Autowired
    private MutableDnsTxtLookupPort dnsTxtLookupPort;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void resetTestPorts() {
        clockPort.setNow(Instant.parse("2026-06-19T00:00:00Z"));
        tokenGeneratorPort.clear();
        dnsTxtLookupPort.clear();
    }

    @Test
    void dev_create_tenant_handler_persists_active_tenant_through_repository_port() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        clockPort.setNow(now);

        var result = createTenantHandler.handle(new CreateTenantCommand("Acme"));

        flushAndClear();
        PersistedTenant found = tenantRepository.findById(result.tenantId()).orElseThrow();

        assertThat(result.name()).isEqualTo("Acme");
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.createdAt()).isEqualTo(now);
        assertThat(found.state().name()).isEqualTo("Acme");
        assertThat(found.state().status()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(tenantRepository.existsActiveById(result.tenantId())).isTrue();
    }

    @Test
    void request_creates_pending_registration_through_repository_port() {
        Instant requestedAt = Instant.parse("2026-06-19T00:00:00Z");
        clockPort.setNow(requestedAt);
        tokenGeneratorPort.enqueue(TOKEN_ONE);

        TenantRegistrationResult result = requestHandler.handle(
                new RequestTenantRegistrationCommand("Acme", "Example.COM")
        );

        flushAndClear();
        PersistedTenantRegistration found = tenantRegistrationRepository.findById(result.registrationId()).orElseThrow();

        assertThat(result.status()).isEqualTo("PENDING_VERIFICATION");
        assertThat(result.domainName()).isEqualTo("example.com");
        assertThat(result.verificationDnsName()).isEqualTo("_pabal-verification.example.com");
        assertThat(result.verificationTxtValue()).isEqualTo("pabal-verification=" + TOKEN_ONE);
        assertThat(result.verificationExpiresAt()).isEqualTo(requestedAt.plus(VERIFICATION_TTL));
        assertThat(result.activationExpiresAt()).isNull();
        assertThat(found.state().status()).isEqualTo(TenantRegistrationStatus.PENDING_VERIFICATION);
        assertThat(found.state().verificationToken()).isEqualTo(TOKEN_ONE);
        assertThat(found.state().version()).isEqualTo(0L);
    }

    @Test
    void request_rejects_open_registration_for_same_domain_in_database() {
        Instant requestedAt = Instant.parse("2026-06-19T00:00:00Z");
        clockPort.setNow(requestedAt);
        tokenGeneratorPort.enqueue(TOKEN_ONE);
        requestHandler.handle(new RequestTenantRegistrationCommand("Acme", "duplicate.example.com"));
        flushAndClear();

        clockPort.setNow(requestedAt.plus(Duration.ofMinutes(1)));

        assertThatThrownBy(() -> requestHandler.handle(
                new RequestTenantRegistrationCommand("Acme 2", "Duplicate.EXAMPLE.com")
        )).isInstanceOf(TenantDomainAlreadyRegisteredException.class);
    }

    @Test
    void request_sweeps_expired_pending_registration_before_reusing_domain() {
        Instant firstRequestedAt = Instant.parse("2026-06-01T00:00:00Z");
        clockPort.setNow(firstRequestedAt);
        tokenGeneratorPort.enqueue(TOKEN_ONE);
        TenantRegistrationResult first = requestHandler.handle(
                new RequestTenantRegistrationCommand("Acme", "reuse.example.com")
        );
        flushAndClear();

        Instant secondRequestedAt = firstRequestedAt.plus(Duration.ofDays(8));
        clockPort.setNow(secondRequestedAt);
        tokenGeneratorPort.enqueue(TOKEN_TWO);
        TenantRegistrationResult second = requestHandler.handle(
                new RequestTenantRegistrationCommand("Acme Renewed", "reuse.example.com")
        );

        flushAndClear();
        PersistedTenantRegistration expired = tenantRegistrationRepository.findById(first.registrationId()).orElseThrow();
        PersistedTenantRegistration replacement = tenantRegistrationRepository.findById(second.registrationId()).orElseThrow();

        assertThat(second.registrationId()).isNotEqualTo(first.registrationId());
        assertThat(expired.state().status()).isEqualTo(TenantRegistrationStatus.EXPIRED);
        assertThat(replacement.state().status()).isEqualTo(TenantRegistrationStatus.PENDING_VERIFICATION);
        assertThat(replacement.state().verificationToken()).isEqualTo(TOKEN_TWO);
    }

    @Test
    void renew_rotates_token_and_extends_expiration_in_database() {
        Instant requestedAt = Instant.parse("2026-06-19T00:00:00Z");
        clockPort.setNow(requestedAt);
        tokenGeneratorPort.enqueue(TOKEN_ONE);
        TenantRegistrationResult requested = requestHandler.handle(
                new RequestTenantRegistrationCommand("Acme", "renew.example.com")
        );
        flushAndClear();

        Instant renewedAt = requestedAt.plus(Duration.ofMinutes(5));
        clockPort.setNow(renewedAt);
        tokenGeneratorPort.enqueue(TOKEN_TWO);
        TenantRegistrationResult renewed = renewHandler.handle(
                new RenewTenantRegistrationTokenCommand(requested.registrationId())
        );

        flushAndClear();
        PersistedTenantRegistration found = tenantRegistrationRepository.findById(requested.registrationId()).orElseThrow();

        assertThat(renewed.verificationTxtValue()).isEqualTo("pabal-verification=" + TOKEN_TWO);
        assertThat(renewed.verificationExpiresAt()).isEqualTo(renewedAt.plus(VERIFICATION_TTL));
        assertThat(found.state().status()).isEqualTo(TenantRegistrationStatus.PENDING_VERIFICATION);
        assertThat(found.state().verificationToken()).isEqualTo(TOKEN_TWO);
        assertThat(found.state().verificationExpiresAt()).isEqualTo(renewedAt.plus(VERIFICATION_TTL));
    }

    @Test
    void renew_expires_elapsed_registration_before_token_rotation() {
        Instant requestedAt = Instant.parse("2026-06-01T00:00:00Z");
        clockPort.setNow(requestedAt);
        tokenGeneratorPort.enqueue(TOKEN_ONE);
        TenantRegistrationResult requested = requestHandler.handle(
                new RequestTenantRegistrationCommand("Acme", "expired-renew.example.com")
        );
        flushAndClear();

        clockPort.setNow(requestedAt.plus(Duration.ofDays(8)));

        assertThatThrownBy(() -> renewHandler.handle(
                new RenewTenantRegistrationTokenCommand(requested.registrationId())
        )).isInstanceOf(TenantRegistrationExpiredException.class);

        flushAndClear();
        PersistedTenantRegistration found = tenantRegistrationRepository.findById(requested.registrationId()).orElseThrow();
        assertThat(found.state().status()).isEqualTo(TenantRegistrationStatus.EXPIRED);
        assertThat(found.state().verificationToken()).isEqualTo(TOKEN_ONE);
    }

    /**
     * Contract: verify is verify-only. It transitions PENDING_VERIFICATION ->
     * DOMAIN_VERIFIED and creates no Tenant row.
     */
    @Test
    void verify_checks_dns_txt_and_marks_domain_verified_without_creating_a_tenant() {
        Instant requestedAt = Instant.parse("2026-06-19T00:00:00Z");
        clockPort.setNow(requestedAt);
        tokenGeneratorPort.enqueue(TOKEN_ONE);
        TenantRegistrationResult requested = requestHandler.handle(
                new RequestTenantRegistrationCommand("Acme", "verify.example.com")
        );
        flushAndClear();

        Instant verifiedAt = requestedAt.plus(Duration.ofMinutes(1));
        clockPort.setNow(verifiedAt);
        dnsTxtLookupPort.setRecords(
                requested.verificationDnsName(),
                Set.of(requested.verificationTxtValue())
        );
        VerifyTenantDomainResult verified = verifyHandler.handle(
                new VerifyTenantDomainCommand(requested.registrationId())
        );

        flushAndClear();
        PersistedTenantRegistration found = tenantRegistrationRepository.findById(requested.registrationId()).orElseThrow();

        assertThat(verified.status()).isEqualTo("DOMAIN_VERIFIED");
        assertThat(verified.verifiedAt()).isEqualTo(verifiedAt);
        assertThat(verified.activationExpiresAt()).isEqualTo(verifiedAt.plus(Duration.ofDays(7)));
        assertThat(found.state().status()).isEqualTo(TenantRegistrationStatus.DOMAIN_VERIFIED);
        assertThat(found.state().tenantId()).isNull();
    }

    @Test
    void verify_keeps_registration_pending_when_dns_txt_does_not_match() {
        Instant requestedAt = Instant.parse("2026-06-19T00:00:00Z");
        clockPort.setNow(requestedAt);
        tokenGeneratorPort.enqueue(TOKEN_ONE);
        TenantRegistrationResult requested = requestHandler.handle(
                new RequestTenantRegistrationCommand("Acme", "missing-dns.example.com")
        );
        flushAndClear();

        clockPort.setNow(requestedAt.plus(Duration.ofMinutes(1)));

        assertThatThrownBy(() -> verifyHandler.handle(
                new VerifyTenantDomainCommand(requested.registrationId())
        )).isInstanceOf(TenantDomainVerificationFailedException.class);

        flushAndClear();
        PersistedTenantRegistration found = tenantRegistrationRepository.findById(requested.registrationId()).orElseThrow();
        assertThat(found.state().status()).isEqualTo(TenantRegistrationStatus.PENDING_VERIFICATION);
        assertThat(found.state().tenantId()).isNull();
    }

    /**
     * Contract: activation on an open-window DOMAIN_VERIFIED registration returns
     * ACTIVATED with a non-null tenantId, and persists exactly one Tenant plus the
     * ACTIVATED registration linked to it.
     */
    @Test
    void activate_creates_tenant_and_links_registration_after_domain_verified() {
        Instant requestedAt = Instant.parse("2026-06-19T00:00:00Z");
        clockPort.setNow(requestedAt);
        tokenGeneratorPort.enqueue(TOKEN_ONE);
        TenantRegistrationResult requested = requestHandler.handle(
                new RequestTenantRegistrationCommand("Acme", "activate.example.com")
        );
        flushAndClear();

        Instant verifiedAt = requestedAt.plus(Duration.ofMinutes(1));
        clockPort.setNow(verifiedAt);
        dnsTxtLookupPort.setRecords(requested.verificationDnsName(), Set.of(requested.verificationTxtValue()));
        verifyHandler.handle(new VerifyTenantDomainCommand(requested.registrationId()));
        flushAndClear();

        Instant activatedAt = verifiedAt.plus(Duration.ofMinutes(1));
        clockPort.setNow(activatedAt);
        ActivateTenantRegistrationResult activated = activateHandler.handle(
                new ActivateTenantRegistrationCommand(requested.registrationId())
        );

        flushAndClear();
        PersistedTenantRegistration found = tenantRegistrationRepository.findById(requested.registrationId()).orElseThrow();

        assertThat(activated.status()).isEqualTo("ACTIVATED");
        assertThat(activated.tenantId()).isNotNull();
        assertThat(activated.verifiedAt()).isEqualTo(verifiedAt);
        assertThat(activated.activatedAt()).isEqualTo(activatedAt);
        assertThat(found.state().status()).isEqualTo(TenantRegistrationStatus.ACTIVATED);
        assertThat(found.state().tenantId()).isEqualTo(activated.tenantId());
        assertThat(tenantRepository.existsActiveById(activated.tenantId())).isTrue();
    }

    /**
     * Contract: activation past the activation window -> TenantRegistrationExpiredException,
     * and no Tenant row is persisted.
     */
    @Test
    void activate_rejects_registration_past_the_activation_window_and_creates_no_tenant() {
        Instant requestedAt = Instant.parse("2026-06-01T00:00:00Z");
        clockPort.setNow(requestedAt);
        tokenGeneratorPort.enqueue(TOKEN_ONE);
        TenantRegistrationResult requested = requestHandler.handle(
                new RequestTenantRegistrationCommand("Acme", "activate-expired.example.com")
        );
        flushAndClear();

        Instant verifiedAt = requestedAt.plus(Duration.ofMinutes(1));
        clockPort.setNow(verifiedAt);
        dnsTxtLookupPort.setRecords(requested.verificationDnsName(), Set.of(requested.verificationTxtValue()));
        verifyHandler.handle(new VerifyTenantDomainCommand(requested.registrationId()));
        flushAndClear();

        clockPort.setNow(verifiedAt.plus(Duration.ofDays(8)));

        assertThatThrownBy(() -> activateHandler.handle(
                new ActivateTenantRegistrationCommand(requested.registrationId())
        )).isInstanceOf(TenantRegistrationExpiredException.class);

        flushAndClear();
        PersistedTenantRegistration found = tenantRegistrationRepository.findById(requested.registrationId()).orElseThrow();
        assertThat(found.state().status()).isEqualTo(TenantRegistrationStatus.DOMAIN_VERIFIED);
        assertThat(found.state().tenantId()).isNull();
    }

    /**
     * Contract: activation when status is not DOMAIN_VERIFIED (e.g. still
     * PENDING_VERIFICATION) -> TenantRegistrationNotPendingException, no Tenant created.
     */
    @Test
    void activate_rejects_registration_still_pending_verification_and_creates_no_tenant() {
        Instant requestedAt = Instant.parse("2026-06-19T00:00:00Z");
        clockPort.setNow(requestedAt);
        tokenGeneratorPort.enqueue(TOKEN_ONE);
        TenantRegistrationResult requested = requestHandler.handle(
                new RequestTenantRegistrationCommand("Acme", "activate-pending.example.com")
        );
        flushAndClear();

        assertThatThrownBy(() -> activateHandler.handle(
                new ActivateTenantRegistrationCommand(requested.registrationId())
        )).isInstanceOf(TenantRegistrationNotPendingException.class);

        flushAndClear();
        PersistedTenantRegistration found = tenantRegistrationRepository.findById(requested.registrationId()).orElseThrow();
        assertThat(found.state().status()).isEqualTo(TenantRegistrationStatus.PENDING_VERIFICATION);
        assertThat(found.state().tenantId()).isNull();
    }

    /**
     * Contract: reverification on REVERIFICATION_REQUIRED with matching DNS TXT returns
     * DOMAIN_VERIFIED with a new activationExpiresAt, transitioned via
     * TenantRegistration.reverify.
     */
    @Test
    void reverifyRegistration_transitions_lapsed_registration_back_to_domain_verified_with_matching_dns() {
        Instant requestedAt = Instant.parse("2026-06-01T00:00:00Z");
        clockPort.setNow(requestedAt);
        tokenGeneratorPort.enqueue(TOKEN_ONE);
        TenantRegistrationResult requested = requestHandler.handle(
                new RequestTenantRegistrationCommand("Acme", "reverify-endpoint.example.com")
        );
        flushAndClear();

        Instant verifiedAt = requestedAt.plus(Duration.ofMinutes(1));
        clockPort.setNow(verifiedAt);
        dnsTxtLookupPort.setRecords(requested.verificationDnsName(), Set.of(requested.verificationTxtValue()));
        verifyHandler.handle(new VerifyTenantDomainCommand(requested.registrationId()));
        flushAndClear();

        // Lapse the activation window via the existing reverify sweep.
        Instant lapsedAt = verifiedAt.plus(Duration.ofDays(8));
        clockPort.setNow(lapsedAt);
        reverifyHandler.handle(new ReverifyLapsedTenantRegistrationsCommand());
        flushAndClear();
        PersistedTenantRegistration lapsed = tenantRegistrationRepository.findById(requested.registrationId()).orElseThrow();
        assertThat(lapsed.state().status()).isEqualTo(TenantRegistrationStatus.REVERIFICATION_REQUIRED);

        Instant reverifiedAt = lapsedAt.plus(Duration.ofMinutes(1));
        clockPort.setNow(reverifiedAt);
        ReverifyTenantRegistrationResult reverified = reverifyRegistrationHandler.handle(
                new ReverifyTenantRegistrationCommand(requested.registrationId())
        );

        flushAndClear();
        PersistedTenantRegistration found = tenantRegistrationRepository.findById(requested.registrationId()).orElseThrow();

        assertThat(reverified.status()).isEqualTo("DOMAIN_VERIFIED");
        assertThat(reverified.verifiedAt()).isEqualTo(reverifiedAt);
        assertThat(reverified.activationExpiresAt()).isEqualTo(reverifiedAt.plus(Duration.ofDays(7)));
        assertThat(found.state().status()).isEqualTo(TenantRegistrationStatus.DOMAIN_VERIFIED);
        assertThat(found.state().activationExpiresAt()).isEqualTo(reverifiedAt.plus(Duration.ofDays(7)));
    }

    /**
     * Contract: reverification when status is not REVERIFICATION_REQUIRED ->
     * TenantRegistrationNotPendingException, with zero DNS lookups performed.
     */
    @Test
    void reverifyRegistration_rejects_registration_not_in_reverification_required_status() {
        Instant requestedAt = Instant.parse("2026-06-19T00:00:00Z");
        clockPort.setNow(requestedAt);
        tokenGeneratorPort.enqueue(TOKEN_ONE);
        TenantRegistrationResult requested = requestHandler.handle(
                new RequestTenantRegistrationCommand("Acme", "reverify-endpoint-guard.example.com")
        );
        flushAndClear();

        assertThatThrownBy(() -> reverifyRegistrationHandler.handle(
                new ReverifyTenantRegistrationCommand(requested.registrationId())
        )).isInstanceOf(TenantRegistrationNotPendingException.class);

        flushAndClear();
        PersistedTenantRegistration found = tenantRegistrationRepository.findById(requested.registrationId()).orElseThrow();
        assertThat(found.state().status()).isEqualTo(TenantRegistrationStatus.PENDING_VERIFICATION);
    }

    @Test
    void poll_reads_pending_rows_from_database_and_updates_each_result_to_domain_verified_never_activated() {
        Instant requestedAt = Instant.parse("2026-06-19T00:00:00Z");
        clockPort.setNow(requestedAt);
        tokenGeneratorPort.enqueue(TOKEN_ONE, TOKEN_TWO);
        TenantRegistrationResult verifiedCandidate = requestHandler.handle(
                new RequestTenantRegistrationCommand("Acme", "poll-verified.example.com")
        );
        TenantRegistrationResult pendingCandidate = requestHandler.handle(
                new RequestTenantRegistrationCommand("Beta", "poll-pending.example.com")
        );
        flushAndClear();

        dnsTxtLookupPort.setRecords(
                verifiedCandidate.verificationDnsName(),
                Set.of(verifiedCandidate.verificationTxtValue())
        );
        clockPort.setNow(requestedAt.plus(Duration.ofMinutes(1)));

        PollTenantDomainVerificationsResult result = pollHandler.handle(
                new PollTenantDomainVerificationsCommand(100)
        );

        flushAndClear();
        PersistedTenantRegistration verified = tenantRegistrationRepository.findById(
                verifiedCandidate.registrationId()
        ).orElseThrow();
        PersistedTenantRegistration pending = tenantRegistrationRepository.findById(
                pendingCandidate.registrationId()
        ).orElseThrow();

        assertThat(result.queuedCount()).isEqualTo(2);
        assertThat(result.verifiedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.expiredCount()).isZero();
        assertThat(result.skippedCount()).isZero();
        // Contract: poll never activates - the matching-DNS candidate lands on
        // DOMAIN_VERIFIED, not ACTIVATED, and no Tenant is created.
        assertThat(verified.state().status()).isEqualTo(TenantRegistrationStatus.DOMAIN_VERIFIED);
        assertThat(pending.state().status()).isEqualTo(TenantRegistrationStatus.PENDING_VERIFICATION);
        assertThat(verified.state().tenantId()).isNull();
        assertThat(pending.state().tenantId()).isNull();
    }

    @Test
    void expire_marks_only_elapsed_pending_rows_in_database() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        PersistedTenantRegistration expiredCandidate = savePendingRegistration(
                "Acme",
                "sweep-expired.example.com",
                TOKEN_ONE,
                now.minus(Duration.ofDays(8)),
                now.minus(Duration.ofDays(1))
        );
        PersistedTenantRegistration openCandidate = savePendingRegistration(
                "Beta",
                "sweep-open.example.com",
                TOKEN_TWO,
                now,
                now.plus(VERIFICATION_TTL)
        );
        flushAndClear();

        clockPort.setNow(now);
        Integer expiredCount = expireHandler.handle(new ExpireTenantRegistrationsCommand());

        flushAndClear();
        PersistedTenantRegistration expired = tenantRegistrationRepository.findById(
                expiredCandidate.state().id()
        ).orElseThrow();
        PersistedTenantRegistration open = tenantRegistrationRepository.findById(
                openCandidate.state().id()
        ).orElseThrow();

        assertThat(expiredCount).isEqualTo(1);
        assertThat(expired.state().status()).isEqualTo(TenantRegistrationStatus.EXPIRED);
        assertThat(open.state().status()).isEqualTo(TenantRegistrationStatus.PENDING_VERIFICATION);
    }

    /**
     * Contract: the reverification sweep transitions exactly the lapsed DOMAIN_VERIFIED
     * rows to REVERIFICATION_REQUIRED through the domain, leaves a non-lapsed
     * DOMAIN_VERIFIED row and a PENDING_VERIFICATION row untouched, and returns the
     * transitioned count.
     */
    @Test
    void reverify_transitions_only_lapsed_domain_verified_rows_in_database() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        PersistedTenantRegistration lapsedCandidate = saveDomainVerifiedRegistration(
                "Acme",
                "reverify-sweep-lapsed.example.com",
                TOKEN_ONE,
                now.minus(Duration.ofDays(10)),
                now.minus(Duration.ofDays(9)),
                now.minus(Duration.ofDays(1))
        );
        PersistedTenantRegistration openDomainVerifiedCandidate = saveDomainVerifiedRegistration(
                "Beta",
                "reverify-sweep-open.example.com",
                TOKEN_TWO,
                now.minus(Duration.ofDays(1)),
                now,
                now.plus(Duration.ofDays(7))
        );
        PersistedTenantRegistration pendingCandidate = savePendingRegistration(
                "Gamma",
                "reverify-sweep-pending.example.com",
                "ABCDEFabcdefghijklmnopqrstuvwxyz",
                now,
                now.plus(VERIFICATION_TTL)
        );
        flushAndClear();

        clockPort.setNow(now);
        Integer transitionedCount = reverifyHandler.handle(new ReverifyLapsedTenantRegistrationsCommand());

        flushAndClear();
        PersistedTenantRegistration lapsed = tenantRegistrationRepository.findById(
                lapsedCandidate.state().id()
        ).orElseThrow();
        PersistedTenantRegistration open = tenantRegistrationRepository.findById(
                openDomainVerifiedCandidate.state().id()
        ).orElseThrow();
        PersistedTenantRegistration pending = tenantRegistrationRepository.findById(
                pendingCandidate.state().id()
        ).orElseThrow();

        assertThat(transitionedCount).isEqualTo(1);
        assertThat(lapsed.state().status()).isEqualTo(TenantRegistrationStatus.REVERIFICATION_REQUIRED);
        assertThat(lapsed.state().updatedAt()).isEqualTo(now);
        assertThat(open.state().status()).isEqualTo(TenantRegistrationStatus.DOMAIN_VERIFIED);
        assertThat(pending.state().status()).isEqualTo(TenantRegistrationStatus.PENDING_VERIFICATION);
    }

    /**
     * Contract: the poll scheduler drives the verify-only handler to DOMAIN_VERIFIED
     * (never ACTIVATED); a subsequent explicit activation call is required to reach
     * ACTIVATED. The pending-expiry scheduler continues to leave a DOMAIN_VERIFIED/
     * ACTIVATED row untouched.
     */
    @Test
    void schedulers_drive_registration_domain_verification_lifecycle_without_activating() {
        Instant requestedAt = Instant.parse("2026-06-19T00:00:00Z");
        clockPort.setNow(requestedAt);
        tokenGeneratorPort.enqueue(TOKEN_ONE);
        TenantRegistrationResult requested = requestHandler.handle(
                new RequestTenantRegistrationCommand("Acme", "scheduler-verify.example.com")
        );
        flushAndClear();

        dnsTxtLookupPort.setRecords(
                requested.verificationDnsName(),
                Set.of(requested.verificationTxtValue())
        );
        clockPort.setNow(requestedAt.plus(Duration.ofMinutes(1)));

        verificationPollScheduler.pollPendingVerifications();

        flushAndClear();
        PersistedTenantRegistration domainVerified = tenantRegistrationRepository.findById(
                requested.registrationId()
        ).orElseThrow();
        assertThat(domainVerified.state().status()).isEqualTo(TenantRegistrationStatus.DOMAIN_VERIFIED);
        assertThat(domainVerified.state().tenantId()).isNull();

        ActivateTenantRegistrationResult activated = activateHandler.handle(
                new ActivateTenantRegistrationCommand(requested.registrationId())
        );
        flushAndClear();
        assertThat(activated.status()).isEqualTo("ACTIVATED");
        assertThat(tenantRepository.existsActiveById(activated.tenantId())).isTrue();

        Instant sweepAt = requestedAt.plus(Duration.ofDays(10));
        PersistedTenantRegistration expiredCandidate = savePendingRegistration(
                "Expired",
                "scheduler-expired.example.com",
                TOKEN_TWO,
                requestedAt,
                requestedAt.plus(VERIFICATION_TTL)
        );
        flushAndClear();
        clockPort.setNow(sweepAt);

        expirationScheduler.expirePendingRegistrations();

        flushAndClear();
        PersistedTenantRegistration expired = tenantRegistrationRepository.findById(
                expiredCandidate.state().id()
        ).orElseThrow();
        PersistedTenantRegistration stillActivated = tenantRegistrationRepository.findById(
                requested.registrationId()
        ).orElseThrow();
        assertThat(expired.state().status()).isEqualTo(TenantRegistrationStatus.EXPIRED);
        assertThat(stillActivated.state().status()).isEqualTo(TenantRegistrationStatus.ACTIVATED);
    }

    private PersistedTenantRegistration saveDomainVerifiedRegistration(
            String tenantName,
            String domainName,
            String token,
            Instant requestedAt,
            Instant verifiedAt,
            Instant activationExpiresAt
    ) {
        TenantRegistration registration = TenantRegistration.request(
                tenantName,
                domainName,
                token,
                requestedAt,
                verifiedAt.plusSeconds(1)
        ).markVerified(verifiedAt, activationExpiresAt);
        return tenantRegistrationRepository.append(
                new PersistedTenantRegistration(
                        registration,
                        TenantRegistrationPersistenceMapper.toState(registration, null)
                )
        );
    }

    private PersistedTenantRegistration savePendingRegistration(
            String tenantName,
            String domainName,
            String token,
            Instant requestedAt,
            Instant expiresAt
    ) {
        TenantRegistration registration = TenantRegistration.request(
                tenantName,
                domainName,
                token,
                requestedAt,
                expiresAt
        );
        return tenantRegistrationRepository.append(
                new PersistedTenantRegistration(
                        registration,
                        TenantRegistrationPersistenceMapper.toState(registration, null)
                )
        );
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @TestConfiguration
    static class TestPorts {

        @Bean
        MutableClockPort clockPort() {
            return new MutableClockPort();
        }

        @Bean
        QueueTokenGeneratorPort tokenGeneratorPort() {
            return new QueueTokenGeneratorPort();
        }

        @Bean
        MutableDnsTxtLookupPort dnsTxtLookupPort() {
            return new MutableDnsTxtLookupPort();
        }
    }

    static class MutableClockPort implements ClockPort {
        private Instant now = Instant.parse("2026-06-19T00:00:00Z");

        void setNow(Instant now) {
            this.now = Objects.requireNonNull(now, "now must not be null");
        }

        @Override
        public Instant now() {
            return now;
        }
    }

    static class QueueTokenGeneratorPort implements TenantVerificationTokenGeneratorPort {
        private final Queue<String> tokens = new ArrayDeque<>();

        void clear() {
            tokens.clear();
        }

        void enqueue(String... tokens) {
            this.tokens.addAll(Arrays.asList(tokens));
        }

        @Override
        public String generate() {
            String token = tokens.poll();
            if (token == null) {
                throw new IllegalStateException("No verification token enqueued");
            }
            return token;
        }
    }

    static class MutableDnsTxtLookupPort implements DnsTxtLookupPort {
        private final Map<String, Set<String>> recordsByName = new HashMap<>();

        void clear() {
            recordsByName.clear();
        }

        void setRecords(String dnsName, Set<String> records) {
            recordsByName.put(dnsName, Set.copyOf(records));
        }

        @Override
        public Set<String> lookupTxtRecords(String dnsName) {
            return recordsByName.getOrDefault(dnsName, Set.of());
        }
    }
}
