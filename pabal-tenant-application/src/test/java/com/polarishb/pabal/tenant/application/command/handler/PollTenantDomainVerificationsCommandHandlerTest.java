package com.polarishb.pabal.tenant.application.command.handler;

import com.polarishb.pabal.tenant.application.command.input.PollTenantDomainVerificationsCommand;
import com.polarishb.pabal.tenant.application.command.input.VerifyTenantDomainCommand;
import com.polarishb.pabal.tenant.application.command.output.PollTenantDomainVerificationsResult;
import com.polarishb.pabal.tenant.application.command.output.VerifyTenantDomainResult;
import com.polarishb.pabal.tenant.application.port.out.persistence.TenantRegistrationRepository;
import com.polarishb.pabal.tenant.application.port.out.time.ClockPort;
import com.polarishb.pabal.tenant.domain.exception.TenantDomainVerificationFailedException;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationExpiredException;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationNotFoundException;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationNotPendingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Contract under test: {@code PollTenantDomainVerificationsCommandHandler} never
 * activates - it only drives the verify-only {@code VerifyTenantDomainCommandHandler}
 * per pending id. After a poll run over a matching-DNS pending registration, that
 * registration transitions to {@code DOMAIN_VERIFIED} (not {@code ACTIVATED}), and
 * {@code verifiedCount} reflects transitions to {@code DOMAIN_VERIFIED} - never
 * activations, since this handler has no knowledge of activation at all.
 */
class PollTenantDomainVerificationsCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-06-19T00:00:00Z");

    private final TenantRegistrationRepository tenantRegistrationRepository = mock(TenantRegistrationRepository.class);
    private final VerifyTenantDomainCommandHandler verifyTenantDomainCommandHandler =
            mock(VerifyTenantDomainCommandHandler.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    private PollTenantDomainVerificationsCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PollTenantDomainVerificationsCommandHandler(
                tenantRegistrationRepository,
                verifyTenantDomainCommandHandler,
                clockPort
        );
    }

    @Test
    void handle_counts_a_successful_verify_as_verifiedCount_with_domain_verified_status_never_activated() {
        UUID registrationId = UUID.randomUUID();
        when(clockPort.now()).thenReturn(NOW);
        when(tenantRegistrationRepository.findPendingVerificationIds(eq(NOW), anyInt()))
                .thenReturn(List.of(registrationId));
        when(verifyTenantDomainCommandHandler.handle(any(VerifyTenantDomainCommand.class)))
                .thenReturn(new VerifyTenantDomainResult(
                        registrationId,
                        "Acme",
                        "example.com",
                        "DOMAIN_VERIFIED",
                        NOW,
                        NOW.plusSeconds(604_800)
                ));

        PollTenantDomainVerificationsResult result = handler.handle(new PollTenantDomainVerificationsCommand(100));

        assertThat(result.queuedCount()).isEqualTo(1);
        assertThat(result.verifiedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isZero();
        assertThat(result.expiredCount()).isZero();
        assertThat(result.skippedCount()).isZero();

        ArgumentCaptor<VerifyTenantDomainCommand> commandCaptor = ArgumentCaptor.forClass(VerifyTenantDomainCommand.class);
        verify(verifyTenantDomainCommandHandler).handle(commandCaptor.capture());
        assertThat(commandCaptor.getValue().registrationId()).isEqualTo(registrationId);
    }

    @Test
    void handle_counts_dns_mismatch_as_failedCount() {
        UUID registrationId = UUID.randomUUID();
        when(clockPort.now()).thenReturn(NOW);
        when(tenantRegistrationRepository.findPendingVerificationIds(eq(NOW), anyInt()))
                .thenReturn(List.of(registrationId));
        when(verifyTenantDomainCommandHandler.handle(any(VerifyTenantDomainCommand.class)))
                .thenThrow(mock(TenantDomainVerificationFailedException.class));

        PollTenantDomainVerificationsResult result = handler.handle(new PollTenantDomainVerificationsCommand(100));

        assertThat(result.verifiedCount()).isZero();
        assertThat(result.failedCount()).isEqualTo(1);
    }

    @Test
    void handle_counts_expired_verification_window_as_expiredCount() {
        UUID registrationId = UUID.randomUUID();
        when(clockPort.now()).thenReturn(NOW);
        when(tenantRegistrationRepository.findPendingVerificationIds(eq(NOW), anyInt()))
                .thenReturn(List.of(registrationId));
        when(verifyTenantDomainCommandHandler.handle(any(VerifyTenantDomainCommand.class)))
                .thenThrow(mock(TenantRegistrationExpiredException.class));

        PollTenantDomainVerificationsResult result = handler.handle(new PollTenantDomainVerificationsCommand(100));

        assertThat(result.verifiedCount()).isZero();
        assertThat(result.expiredCount()).isEqualTo(1);
    }

    @Test
    void handle_counts_not_found_and_not_pending_as_skippedCount() {
        UUID notFoundId = UUID.randomUUID();
        UUID notPendingId = UUID.randomUUID();
        when(clockPort.now()).thenReturn(NOW);
        when(tenantRegistrationRepository.findPendingVerificationIds(eq(NOW), anyInt()))
                .thenReturn(List.of(notFoundId, notPendingId));
        when(verifyTenantDomainCommandHandler.handle(new VerifyTenantDomainCommand(notFoundId)))
                .thenThrow(mock(TenantRegistrationNotFoundException.class));
        when(verifyTenantDomainCommandHandler.handle(new VerifyTenantDomainCommand(notPendingId)))
                .thenThrow(mock(TenantRegistrationNotPendingException.class));

        PollTenantDomainVerificationsResult result = handler.handle(new PollTenantDomainVerificationsCommand(100));

        assertThat(result.queuedCount()).isEqualTo(2);
        assertThat(result.skippedCount()).isEqualTo(2);
        assertThat(result.verifiedCount()).isZero();
    }
}
