package com.polarishb.pabal.tenant.domain.model;

import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationExpiredException;
import com.polarishb.pabal.tenant.domain.exception.TenantRegistrationNotPendingException;
import com.polarishb.pabal.tenant.domain.model.snapshot.TenantRegistrationSnapshot;
import com.polarishb.pabal.tenant.domain.model.type.TenantRegistrationStatus;
import com.polarishb.pabal.tenant.domain.model.vo.TenantDomainName;
import com.polarishb.pabal.tenant.domain.model.vo.TenantName;
import com.polarishb.pabal.tenant.domain.model.vo.TenantVerificationToken;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TenantRegistration {

    private static final String VERIFICATION_DNS_PREFIX = "_pabal-verification.";
    private static final String VERIFICATION_TXT_PREFIX = "pabal-verification=";

    @EqualsAndHashCode.Include
    private final UUID id;
    private final TenantName tenantName;
    private final TenantDomainName domainName;
    private final TenantVerificationToken verificationToken;
    private final TenantRegistrationStatus status;
    private final Instant expiresAt;
    private final Instant verifiedAt;
    private final Instant activatedAt;
    private final UUID tenantId;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static TenantRegistration request(
            String tenantName,
            String domainName,
            String verificationToken,
            Instant requestedAt,
            Instant expiresAt
    ) {
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        if (!expiresAt.isAfter(requestedAt)) {
            throw new IllegalArgumentException("expiresAt must be after requestedAt");
        }

        return new TenantRegistration(
                null,
                new TenantName(tenantName),
                new TenantDomainName(domainName),
                new TenantVerificationToken(verificationToken),
                TenantRegistrationStatus.PENDING_VERIFICATION,
                expiresAt,
                null,
                null,
                null,
                requestedAt,
                requestedAt
        );
    }

    public static TenantRegistration reconstitute(TenantRegistrationSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        return new TenantRegistration(
                snapshot.id(),
                snapshot.tenantName(),
                snapshot.domainName(),
                snapshot.verificationToken(),
                snapshot.status(),
                snapshot.expiresAt(),
                snapshot.verifiedAt(),
                snapshot.activatedAt(),
                snapshot.tenantId(),
                snapshot.createdAt(),
                snapshot.updatedAt()
        );
    }

    public TenantRegistrationSnapshot snapshot() {
        return new TenantRegistrationSnapshot(
                id,
                tenantName,
                domainName,
                verificationToken,
                status,
                expiresAt,
                verifiedAt,
                activatedAt,
                tenantId,
                createdAt,
                updatedAt
        );
    }

    public String verificationDnsName() {
        return VERIFICATION_DNS_PREFIX + domainName.value();
    }

    public String verificationTxtValue() {
        return VERIFICATION_TXT_PREFIX + verificationToken.value();
    }

    public boolean matchesTxtValue(String txtValue) {
        return verificationTxtValue().equals(normalizeTxtValue(txtValue));
    }

    public void validateVerificationAllowed(Instant now) {
        ensurePending(now);
    }

    public TenantRegistration renewVerificationToken(String verificationToken, Instant renewedAt, Instant expiresAt) {
        Objects.requireNonNull(renewedAt, "renewedAt must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        ensurePending(renewedAt);
        if (!expiresAt.isAfter(renewedAt)) {
            throw new IllegalArgumentException("expiresAt must be after renewedAt");
        }

        return new TenantRegistration(
                id,
                tenantName,
                domainName,
                new TenantVerificationToken(verificationToken),
                status,
                expiresAt,
                verifiedAt,
                activatedAt,
                tenantId,
                createdAt,
                renewedAt
        );
    }

    public TenantRegistration markVerified(Instant verifiedAt) {
        ensurePending(verifiedAt);
        Instant transitionAt = Objects.requireNonNull(verifiedAt, "verifiedAt must not be null");
        return new TenantRegistration(
                id,
                tenantName,
                domainName,
                verificationToken,
                TenantRegistrationStatus.VERIFIED,
                expiresAt,
                transitionAt,
                activatedAt,
                tenantId,
                createdAt,
                transitionAt
        );
    }

    public TenantRegistration activate(UUID tenantId, Instant activatedAt) {
        if (status != TenantRegistrationStatus.VERIFIED) {
            throw new TenantRegistrationNotPendingException(id, status);
        }
        UUID activatedTenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        Instant transitionAt = Objects.requireNonNull(activatedAt, "activatedAt must not be null");
        return new TenantRegistration(
                id,
                tenantName,
                domainName,
                verificationToken,
                TenantRegistrationStatus.ACTIVATED,
                expiresAt,
                verifiedAt,
                transitionAt,
                activatedTenantId,
                createdAt,
                transitionAt
        );
    }

    public TenantRegistration expire(Instant expiredAt) {
        Objects.requireNonNull(expiredAt, "expiredAt must not be null");
        if (status != TenantRegistrationStatus.PENDING_VERIFICATION) {
            throw new TenantRegistrationNotPendingException(id, status);
        }
        return new TenantRegistration(
                id,
                tenantName,
                domainName,
                verificationToken,
                TenantRegistrationStatus.EXPIRED,
                expiresAt,
                verifiedAt,
                activatedAt,
                tenantId,
                createdAt,
                expiredAt
        );
    }

    public boolean isOpen() {
        return status == TenantRegistrationStatus.PENDING_VERIFICATION
                || status == TenantRegistrationStatus.VERIFIED
                || status == TenantRegistrationStatus.ACTIVATED;
    }

    private void ensurePending(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (status != TenantRegistrationStatus.PENDING_VERIFICATION) {
            throw new TenantRegistrationNotPendingException(id, status);
        }
        if (!now.isBefore(expiresAt)) {
            throw new TenantRegistrationExpiredException(id, expiresAt);
        }
    }

    private String normalizeTxtValue(String txtValue) {
        if (txtValue == null) {
            return "";
        }
        String normalized = txtValue.trim();
        if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized;
    }
}
