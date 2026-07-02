package com.polarishb.pabal.tenant.domain.model;

import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationExpiredException;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationNotPendingException;
import com.polarishb.pabal.tenant.domain.model.snapshot.TenantRegistrationSnapshot;
import com.polarishb.pabal.tenant.domain.model.type.TenantRegistrationStatus;
import com.polarishb.pabal.tenant.domain.model.vo.TenantDomainName;
import com.polarishb.pabal.tenant.domain.model.vo.TenantName;
import com.polarishb.pabal.tenant.domain.model.vo.TenantVerificationToken;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract under test: split {@code expiresAt} into {@code verificationExpiresAt} /
 * {@code activationExpiresAt}, and refine status into
 * {PENDING_VERIFICATION, DOMAIN_VERIFIED, REVERIFICATION_REQUIRED, ACTIVATED, EXPIRED}.
 *
 * <p>Existing assertions against the old single-{@code expiresAt} / {@code VERIFIED}
 * shape have been rewritten to the new contract shape; they previously encoded behavior
 * this contract deliberately supersedes.
 */
class TenantRegistrationTest {

    private static final String TOKEN = "abcdefghijklmnopqrstuvwxyzABCDEF";

    @Test
    void request_creates_pending_registration_with_dns_instructions() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");

        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "Example.COM",
                TOKEN,
                now,
                now.plusSeconds(60)
        );

        assertThat(registration.getStatus()).isEqualTo(TenantRegistrationStatus.PENDING_VERIFICATION);
        assertThat(registration.getTenantName().value()).isEqualTo("Acme");
        assertThat(registration.getDomainName().value()).isEqualTo("example.com");
        assertThat(registration.getVerificationExpiresAt()).isEqualTo(now.plusSeconds(60));
        assertThat(registration.getActivationExpiresAt()).isNull();
        assertThat(registration.verificationDnsName()).isEqualTo("_pabal-verification.example.com");
        assertThat(registration.verificationTxtValue()).isEqualTo("pabal-verification=" + TOKEN);
    }

    @Test
    void request_rejects_verificationExpiresAt_not_after_requestedAt() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");

        assertThatThrownBy(() -> TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now.minusSeconds(1)
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void matchesTxtValue_accepts_quoted_dns_record_value() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now.plusSeconds(60)
        );

        assertThat(registration.matchesTxtValue("\"pabal-verification=" + TOKEN + "\"")).isTrue();
    }

    @Test
    void renewVerificationToken_rotates_token_and_extends_verificationExpiresAt() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        String renewedToken = "ABCDEFabcdefghijklmnopqrstuvwxyz";
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now.plusSeconds(60)
        );

        TenantRegistration renewed = registration.renewVerificationToken(
                renewedToken,
                now.plusSeconds(10),
                now.plusSeconds(600)
        );

        assertThat(renewed).isNotSameAs(registration);
        assertThat(registration.verificationTxtValue()).isEqualTo("pabal-verification=" + TOKEN);
        assertThat(registration.getVerificationExpiresAt()).isEqualTo(now.plusSeconds(60));
        assertThat(renewed.verificationTxtValue()).isEqualTo("pabal-verification=" + renewedToken);
        assertThat(renewed.getVerificationExpiresAt()).isEqualTo(now.plusSeconds(600));
        assertThat(renewed.getUpdatedAt()).isEqualTo(now.plusSeconds(10));
    }

    // --- markVerified ---------------------------------------------------

    @Test
    void markVerified_transitions_pending_to_domain_verified_and_sets_activationExpiresAt() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now.plusSeconds(60)
        );

        Instant verifiedAt = now.plusSeconds(1);
        Instant activationExpiresAt = verifiedAt.plusSeconds(3600);
        TenantRegistration verified = registration.markVerified(verifiedAt, activationExpiresAt);

        assertThat(verified).isNotSameAs(registration);
        assertThat(registration.getStatus()).isEqualTo(TenantRegistrationStatus.PENDING_VERIFICATION);
        assertThat(verified.getStatus()).isEqualTo(TenantRegistrationStatus.DOMAIN_VERIFIED);
        assertThat(verified.getVerifiedAt()).isEqualTo(verifiedAt);
        assertThat(verified.getActivationExpiresAt()).isEqualTo(activationExpiresAt);
    }

    @Test
    void markVerified_rejects_activationExpiresAt_not_after_verifiedAt() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now.plusSeconds(60)
        );

        Instant verifiedAt = now.plusSeconds(1);

        assertThatThrownBy(() -> registration.markVerified(verifiedAt, verifiedAt))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registration.markVerified(verifiedAt, verifiedAt.minusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void markVerified_rejects_when_verificationExpiresAt_has_lapsed() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now.plusSeconds(60)
        );

        // verifiedAt == verificationExpiresAt: not strictly before -> expired
        assertThatThrownBy(() -> registration.markVerified(now.plusSeconds(60), now.plusSeconds(3660)))
                .isInstanceOf(TenantRegistrationExpiredException.class);

        // verifiedAt after verificationExpiresAt -> expired
        assertThatThrownBy(() -> registration.markVerified(now.plusSeconds(61), now.plusSeconds(3661)))
                .isInstanceOf(TenantRegistrationExpiredException.class);
    }

    @Test
    void markVerified_rejects_non_pending_registration() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now.plusSeconds(60)
        );
        Instant verifiedAt = now.plusSeconds(1);
        TenantRegistration verified = registration.markVerified(verifiedAt, verifiedAt.plusSeconds(3600));

        assertThatThrownBy(() -> verified.markVerified(verifiedAt.plusSeconds(2), verifiedAt.plusSeconds(3700)))
                .isInstanceOf(TenantRegistrationNotPendingException.class);
    }

    // --- requireReverification -------------------------------------------

    @Test
    void requireReverification_transitions_lapsed_domain_verified_to_reverification_required() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now.plusSeconds(60)
        );
        Instant verifiedAt = now.plusSeconds(1);
        Instant activationExpiresAt = verifiedAt.plusSeconds(3600);
        TenantRegistration verified = registration.markVerified(verifiedAt, activationExpiresAt);

        Instant lapsedNow = activationExpiresAt.plusSeconds(1);
        TenantRegistration lapsed = verified.requireReverification(lapsedNow);

        assertThat(lapsed).isNotSameAs(verified);
        assertThat(lapsed.getStatus()).isEqualTo(TenantRegistrationStatus.REVERIFICATION_REQUIRED);
        assertThat(lapsed.getUpdatedAt()).isEqualTo(lapsedNow);
        // verifiedAt and activationExpiresAt are preserved (now-lapsed window observable)
        assertThat(lapsed.getVerifiedAt()).isEqualTo(verifiedAt);
        assertThat(lapsed.getActivationExpiresAt()).isEqualTo(activationExpiresAt);
    }

    @Test
    void requireReverification_at_exact_activationExpiresAt_boundary_is_lapsed() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now.plusSeconds(60)
        );
        Instant verifiedAt = now.plusSeconds(1);
        Instant activationExpiresAt = verifiedAt.plusSeconds(3600);
        TenantRegistration verified = registration.markVerified(verifiedAt, activationExpiresAt);

        TenantRegistration lapsed = verified.requireReverification(activationExpiresAt);

        assertThat(lapsed.getStatus()).isEqualTo(TenantRegistrationStatus.REVERIFICATION_REQUIRED);
    }

    @Test
    void requireReverification_rejects_when_activation_window_still_open() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now.plusSeconds(60)
        );
        Instant verifiedAt = now.plusSeconds(1);
        Instant activationExpiresAt = verifiedAt.plusSeconds(3600);
        TenantRegistration verified = registration.markVerified(verifiedAt, activationExpiresAt);

        assertThatThrownBy(() -> verified.requireReverification(activationExpiresAt.minusSeconds(1)))
                .isInstanceOf(IllegalStateException.class)
                .message().isNotBlank();
    }

    @Test
    void requireReverification_rejects_non_domain_verified_registration() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now.plusSeconds(60)
        );

        // still PENDING_VERIFICATION
        assertThatThrownBy(() -> registration.requireReverification(now.plusSeconds(10)))
                .isInstanceOf(TenantRegistrationNotPendingException.class);
    }

    // --- reverify ---------------------------------------------------------

    @Test
    void reverify_transitions_reverification_required_back_to_domain_verified_with_refreshed_activationExpiresAt() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now.plusSeconds(60)
        );
        Instant verifiedAt = now.plusSeconds(1);
        Instant activationExpiresAt = verifiedAt.plusSeconds(3600);
        TenantRegistration verified = registration.markVerified(verifiedAt, activationExpiresAt);
        TenantRegistration lapsed = verified.requireReverification(activationExpiresAt.plusSeconds(1));

        Instant reverifiedAt = activationExpiresAt.plusSeconds(2);
        Instant newActivationExpiresAt = reverifiedAt.plusSeconds(3600);
        TenantRegistration reverified = lapsed.reverify(reverifiedAt, newActivationExpiresAt);

        assertThat(reverified).isNotSameAs(lapsed);
        assertThat(reverified.getStatus()).isEqualTo(TenantRegistrationStatus.DOMAIN_VERIFIED);
        assertThat(reverified.getVerifiedAt()).isEqualTo(reverifiedAt);
        assertThat(reverified.getActivationExpiresAt()).isEqualTo(newActivationExpiresAt);
    }

    @Test
    void reverify_rejects_activationExpiresAt_not_after_reverifiedAt() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now.plusSeconds(60)
        );
        Instant verifiedAt = now.plusSeconds(1);
        Instant activationExpiresAt = verifiedAt.plusSeconds(3600);
        TenantRegistration verified = registration.markVerified(verifiedAt, activationExpiresAt);
        TenantRegistration lapsed = verified.requireReverification(activationExpiresAt.plusSeconds(1));

        Instant reverifiedAt = activationExpiresAt.plusSeconds(2);

        assertThatThrownBy(() -> lapsed.reverify(reverifiedAt, reverifiedAt))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> lapsed.reverify(reverifiedAt, reverifiedAt.minusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reverify_rejects_non_reverification_required_registration() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now.plusSeconds(60)
        );
        Instant verifiedAt = now.plusSeconds(1);
        Instant activationExpiresAt = verifiedAt.plusSeconds(3600);
        TenantRegistration verified = registration.markVerified(verifiedAt, activationExpiresAt);

        // still DOMAIN_VERIFIED, not REVERIFICATION_REQUIRED
        assertThatThrownBy(() -> verified.reverify(verifiedAt.plusSeconds(1), verifiedAt.plusSeconds(1).plusSeconds(3600)))
                .isInstanceOf(TenantRegistrationNotPendingException.class);

        // PENDING_VERIFICATION
        assertThatThrownBy(() -> registration.reverify(now.plusSeconds(1), now.plusSeconds(3601)))
                .isInstanceOf(TenantRegistrationNotPendingException.class);
    }

    // --- activate -----------------------------------------------------------

    @Test
    void activate_from_domain_verified_before_activationExpiresAt_sets_tenant_id_and_activated_status() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        UUID tenantId = UUID.randomUUID();
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now.plusSeconds(60)
        );

        Instant verifiedAt = now.plusSeconds(1);
        Instant activationExpiresAt = verifiedAt.plusSeconds(3600);
        TenantRegistration verified = registration.markVerified(verifiedAt, activationExpiresAt);

        Instant activatedAt = activationExpiresAt.minusSeconds(1);
        TenantRegistration activated = verified.activate(tenantId, activatedAt);

        assertThat(activated).isNotSameAs(verified);
        assertThat(registration.getStatus()).isEqualTo(TenantRegistrationStatus.PENDING_VERIFICATION);
        assertThat(verified.getStatus()).isEqualTo(TenantRegistrationStatus.DOMAIN_VERIFIED);
        assertThat(activated.getStatus()).isEqualTo(TenantRegistrationStatus.ACTIVATED);
        assertThat(activated.getTenantId()).isEqualTo(tenantId);
        assertThat(activated.getActivatedAt()).isEqualTo(activatedAt);
    }

    @Test
    void activate_rejects_at_activationExpiresAt_boundary() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        UUID tenantId = UUID.randomUUID();
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now.plusSeconds(60)
        );
        Instant verifiedAt = now.plusSeconds(1);
        Instant activationExpiresAt = verifiedAt.plusSeconds(3600);
        TenantRegistration verified = registration.markVerified(verifiedAt, activationExpiresAt);

        assertThatThrownBy(() -> verified.activate(tenantId, activationExpiresAt))
                .isInstanceOf(TenantRegistrationExpiredException.class);
    }

    @Test
    void activate_rejects_after_activationExpiresAt() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        UUID tenantId = UUID.randomUUID();
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now.plusSeconds(60)
        );
        Instant verifiedAt = now.plusSeconds(1);
        Instant activationExpiresAt = verifiedAt.plusSeconds(3600);
        TenantRegistration verified = registration.markVerified(verifiedAt, activationExpiresAt);

        assertThatThrownBy(() -> verified.activate(tenantId, activationExpiresAt.plusSeconds(1)))
                .isInstanceOf(TenantRegistrationExpiredException.class);
    }

    @Test
    void activate_rejects_from_reverification_required_state() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        UUID tenantId = UUID.randomUUID();
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now.plusSeconds(60)
        );
        Instant verifiedAt = now.plusSeconds(1);
        Instant activationExpiresAt = verifiedAt.plusSeconds(3600);
        TenantRegistration verified = registration.markVerified(verifiedAt, activationExpiresAt);
        TenantRegistration lapsed = verified.requireReverification(activationExpiresAt.plusSeconds(1));

        assertThatThrownBy(() -> lapsed.activate(tenantId, activationExpiresAt.plusSeconds(2)))
                .isInstanceOf(TenantRegistrationNotPendingException.class);
    }

    @Test
    void activate_rejects_from_pending_verification_state() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        UUID tenantId = UUID.randomUUID();
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now.plusSeconds(60)
        );

        assertThatThrownBy(() -> registration.activate(tenantId, now.plusSeconds(1)))
                .isInstanceOf(TenantRegistrationNotPendingException.class);
    }

    // --- expire -------------------------------------------------------------

    @Test
    void expire_transitions_pending_verification_to_expired() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now.plusSeconds(60)
        );

        TenantRegistration expired = registration.expire(now.plusSeconds(100));

        assertThat(expired).isNotSameAs(registration);
        assertThat(expired.getStatus()).isEqualTo(TenantRegistrationStatus.EXPIRED);
    }

    @Test
    void expire_transitions_domain_verified_to_expired() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now.plusSeconds(60)
        );
        Instant verifiedAt = now.plusSeconds(1);
        TenantRegistration verified = registration.markVerified(verifiedAt, verifiedAt.plusSeconds(3600));

        TenantRegistration expired = verified.expire(verifiedAt.plusSeconds(10));

        assertThat(expired.getStatus()).isEqualTo(TenantRegistrationStatus.EXPIRED);
    }

    @Test
    void expire_transitions_reverification_required_to_expired() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now.plusSeconds(60)
        );
        Instant verifiedAt = now.plusSeconds(1);
        Instant activationExpiresAt = verifiedAt.plusSeconds(3600);
        TenantRegistration verified = registration.markVerified(verifiedAt, activationExpiresAt);
        TenantRegistration lapsed = verified.requireReverification(activationExpiresAt.plusSeconds(1));

        TenantRegistration expired = lapsed.expire(activationExpiresAt.plusSeconds(10));

        assertThat(expired.getStatus()).isEqualTo(TenantRegistrationStatus.EXPIRED);
    }

    // --- isOpen ---------------------------------------------------------------

    @Test
    void isOpen_is_true_for_pending_domainVerified_reverificationRequired_and_activated() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now.plusSeconds(60)
        );
        assertThat(registration.isOpen()).isTrue();

        Instant verifiedAt = now.plusSeconds(1);
        Instant activationExpiresAt = verifiedAt.plusSeconds(3600);
        TenantRegistration verified = registration.markVerified(verifiedAt, activationExpiresAt);
        assertThat(verified.isOpen()).isTrue();

        TenantRegistration lapsed = verified.requireReverification(activationExpiresAt.plusSeconds(1));
        assertThat(lapsed.isOpen()).isTrue();

        TenantRegistration activated = verified.activate(UUID.randomUUID(), activationExpiresAt.minusSeconds(1));
        assertThat(activated.isOpen()).isTrue();
    }

    @Test
    void isOpen_is_false_for_expired() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now.plusSeconds(60)
        );
        TenantRegistration expired = registration.expire(now.plusSeconds(100));

        assertThat(expired.isOpen()).isFalse();
    }

    // --- snapshot / reconstitute round-trip -------------------------------------

    @Test
    void snapshot_and_reconstitute_round_trip_both_expiry_fields() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        TenantRegistration registration = TenantRegistration.request(
                "Acme",
                "example.com",
                TOKEN,
                now,
                now.plusSeconds(60)
        );
        Instant verifiedAt = now.plusSeconds(1);
        Instant activationExpiresAt = verifiedAt.plusSeconds(3600);
        TenantRegistration verified = registration.markVerified(verifiedAt, activationExpiresAt);

        TenantRegistrationSnapshot snapshot = verified.snapshot();
        assertThat(snapshot.verificationExpiresAt()).isEqualTo(now.plusSeconds(60));
        assertThat(snapshot.activationExpiresAt()).isEqualTo(activationExpiresAt);

        TenantRegistration reconstituted = TenantRegistration.reconstitute(snapshot);
        assertThat(reconstituted.getStatus()).isEqualTo(TenantRegistrationStatus.DOMAIN_VERIFIED);
        assertThat(reconstituted.getVerificationExpiresAt()).isEqualTo(now.plusSeconds(60));
        assertThat(reconstituted.getActivationExpiresAt()).isEqualTo(activationExpiresAt);
        assertThat(reconstituted.getVerifiedAt()).isEqualTo(verifiedAt);
    }

    @Test
    void snapshot_requires_createdAt_and_updatedAt_for_persistence_reconstitution() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");

        assertThatThrownBy(() -> snapshot(null, now))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("createdAt must not be null");
        assertThatThrownBy(() -> snapshot(now, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("updatedAt must not be null");
    }

    private TenantRegistrationSnapshot snapshot(Instant createdAt, Instant updatedAt) {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        return new TenantRegistrationSnapshot(
                UUID.randomUUID(),
                new TenantName("Acme"),
                new TenantDomainName("example.com"),
                new TenantVerificationToken(TOKEN),
                TenantRegistrationStatus.PENDING_VERIFICATION,
                now.plusSeconds(60), // verificationExpiresAt
                null,                // activationExpiresAt
                null,                // verifiedAt
                null,                // activatedAt
                null,                // tenantId
                createdAt,
                updatedAt
        );
    }
}
