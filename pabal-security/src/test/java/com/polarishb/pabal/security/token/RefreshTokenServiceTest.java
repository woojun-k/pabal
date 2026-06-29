package com.polarishb.pabal.security.token;

import com.polarishb.pabal.security.config.JwtSecurityProperties;
import com.polarishb.pabal.security.config.RefreshTokenReplayProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenServiceTest {

    private final JwtSecurityProperties properties = new JwtSecurityProperties(
            "local-dev",
            "pabal-api",
            "uid",
            "tenant_id",
            "sub",
            Duration.ofSeconds(30),
            Duration.ofMinutes(60),
            Duration.ofMinutes(90),
            Duration.ofDays(7)
    );
    private final RefreshTokenReplayProperties replayProperties = new RefreshTokenReplayProperties(
            Duration.ofSeconds(3),
            Duration.ofSeconds(30)
    );

    @Test
    void issueTokenPair_issues_short_lived_access_token_and_persisted_refresh_token() {
        InMemoryRefreshTokenStore store = new InMemoryRefreshTokenStore();
        FakeJwtEncoder encoder = new FakeJwtEncoder();
        RefreshTokenService service = service(encoder, store);

        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Instant before = Instant.now();

        IssuedTokenPair tokenPair = service.issueTokenPair(
                userId,
                tenantId,
                "subject",
                JwtAuthorityClaims.of(List.of("messenger:channel:create"), List.of("tenant_admin"), List.of())
        );

        assertThat(tokenPair.tokenType()).isEqualTo("Bearer");
        assertThat(tokenPair.accessToken()).isEqualTo("access-1");
        assertThat(tokenPair.refreshToken()).isNotBlank();
        assertThat(tokenPair.accessTokenExpiresAt()).isBetween(
                before.plus(Duration.ofMinutes(60)),
                before.plus(Duration.ofMinutes(91))
        );
        assertThat(encoder.lastExpiresAt()).isEqualTo(tokenPair.accessTokenExpiresAt());
        assertThat(tokenPair.refreshTokenExpiresAt()).isAfter(before.plus(Duration.ofDays(6)));
        assertThat(store.records).hasSize(1);
        assertThat(encoder.lastClaims())
                .containsEntry("uid", userId.toString())
                .containsEntry("tenant_id", tenantId.toString())
                .containsEntry("scope", "messenger:channel:create")
                .containsEntry("roles", List.of("tenant_admin"));
    }

    @Test
    void issueTokenPair_omits_empty_authority_claims_from_jwt() {
        InMemoryRefreshTokenStore store = new InMemoryRefreshTokenStore();
        FakeJwtEncoder encoder = new FakeJwtEncoder();
        RefreshTokenService service = service(encoder, store);

        service.issueTokenPair(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "subject",
                JwtAuthorityClaims.empty()
        );

        assertThat(encoder.lastClaims())
                .doesNotContainKeys("scope", "roles", "permissions");
    }

    @Test
    void refresh_rotates_refresh_token_and_revokes_previous_token() {
        InMemoryRefreshTokenStore store = new InMemoryRefreshTokenStore();
        FakeJwtEncoder encoder = new FakeJwtEncoder();
        RefreshTokenService service = service(encoder, store);

        IssuedTokenPair first = service.issueTokenPair(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "subject",
                JwtAuthorityClaims.empty()
        );

        IssuedTokenPair second = service.refresh(first.refreshToken());

        assertThat(second.accessToken()).isEqualTo("access-2");
        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThat(store.records.values())
                .filteredOn(record -> record.replacedByTokenId() != null)
                .hasSize(1);
        assertThat(store.findByRawToken(first.refreshToken()).orElseThrow().usedAt()).isNotNull();
    }

    @Test
    void refresh_returns_cached_token_pair_when_used_token_is_replayed_within_grace_period() {
        InMemoryRefreshTokenStore store = new InMemoryRefreshTokenStore();
        FakeJwtEncoder encoder = new FakeJwtEncoder();
        RefreshTokenService service = service(encoder, store);
        IssuedTokenPair first = service.issueTokenPair(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "subject",
                JwtAuthorityClaims.empty()
        );
        IssuedTokenPair second = service.refresh(first.refreshToken());

        IssuedTokenPair replayed = service.refresh(first.refreshToken());

        assertThat(replayed).isEqualTo(second);
        assertThat(encoder.issuedClaims).hasSize(2);
        assertThat(store.revokeTokenFamilyCalls).isZero();
    }

    @Test
    void concurrent_refresh_of_same_token_returns_single_rotation_and_grace_replay_without_family_revocation()
            throws Exception {
        InMemoryRefreshTokenStore store = new InMemoryRefreshTokenStore();
        InMemoryRefreshTokenReplayCache replayCache = new InMemoryRefreshTokenReplayCache();
        CountDownLatch usedTokenReplaySaved = new CountDownLatch(1);
        store.synchronizeNextFindByTokenHash(2);
        store.waitForReplayBeforeFailedMark(usedTokenReplaySaved);
        replayCache.signalUsedTokenReplaySave(usedTokenReplaySaved);

        FakeJwtEncoder encoder = new FakeJwtEncoder();
        RefreshTokenService service = service(encoder, store, replayCache);
        IssuedTokenPair first = service.issueTokenPair(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "subject",
                JwtAuthorityClaims.empty()
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<IssuedTokenPair> firstAttempt = executor.submit(() -> service.refresh(first.refreshToken()));
            Future<IssuedTokenPair> secondAttempt = executor.submit(() -> service.refresh(first.refreshToken()));

            IssuedTokenPair firstResult = await(firstAttempt);
            IssuedTokenPair secondResult = await(secondAttempt);

            assertThat(firstResult).isEqualTo(secondResult);
            assertThat(firstResult.refreshToken()).isNotEqualTo(first.refreshToken());
            assertThat(encoder.issuedClaims).hasSize(2);
            assertThat(store.markUsedAndRevokeCalls).isEqualTo(2);
            assertThat(store.revokeCalls).isEqualTo(1);
            assertThat(store.revokeTokenFamilyCalls).isZero();
            assertThat(store.activeRefreshTokenCount()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void refresh_returns_request_id_cached_token_pair_without_store_lookup() {
        InMemoryRefreshTokenStore store = new InMemoryRefreshTokenStore();
        FakeJwtEncoder encoder = new FakeJwtEncoder();
        RefreshTokenService service = service(encoder, store);
        IssuedTokenPair first = service.issueTokenPair(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "subject",
                JwtAuthorityClaims.empty()
        );
        IssuedTokenPair second = service.refresh(first.refreshToken(), "request-1");
        int findCalls = store.findByTokenHashCalls;

        IssuedTokenPair replayed = service.refresh(first.refreshToken(), "request-1");

        assertThat(replayed).isEqualTo(second);
        assertThat(store.findByTokenHashCalls).isEqualTo(findCalls);
        assertThat(encoder.issuedClaims).hasSize(2);
    }

    @Test
    void refresh_does_not_revoke_family_when_grace_replay_cache_is_missing() {
        InMemoryRefreshTokenStore store = new InMemoryRefreshTokenStore();
        RefreshTokenService service = service(
                new FakeJwtEncoder(),
                store,
                new NoopRefreshTokenReplayCache()
        );
        IssuedTokenPair first = service.issueTokenPair(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "subject",
                JwtAuthorityClaims.empty()
        );
        IssuedTokenPair second = service.refresh(first.refreshToken());

        assertThatThrownBy(() -> service.refresh(first.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token replay is not available");

        assertThat(store.revokeTokenFamilyCalls).isZero();
        assertThatCode(() -> service.refresh(second.refreshToken()))
                .doesNotThrowAnyException();
    }

    @Test
    void refresh_revokes_replacement_token_family_when_reuse_is_detected_after_grace_period() {
        InMemoryRefreshTokenStore store = new InMemoryRefreshTokenStore();
        RefreshTokenReplayProperties noGraceProperties = new RefreshTokenReplayProperties(
                Duration.ZERO,
                Duration.ofSeconds(30)
        );
        RefreshTokenService service = service(new FakeJwtEncoder(), store, noGraceProperties);
        IssuedTokenPair first = service.issueTokenPair(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "subject",
                JwtAuthorityClaims.empty()
        );
        IssuedTokenPair second = service.refresh(first.refreshToken());

        assertThatThrownBy(() -> service.refresh(first.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token is expired or revoked");

        assertThatThrownBy(() -> service.refresh(second.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token is expired or revoked");
        assertThat(store.revokeTokenFamilyCalls).isEqualTo(1);
    }

    @Test
    void refresh_carries_authority_claims_from_original_token() {
        InMemoryRefreshTokenStore store = new InMemoryRefreshTokenStore();
        FakeJwtEncoder encoder = new FakeJwtEncoder();
        RefreshTokenService service = service(encoder, store);
        JwtAuthorityClaims claims = JwtAuthorityClaims.of(
                List.of("messenger:channel:create"),
                List.of("tenant_admin"),
                List.of("messenger:channel:delete:any")
        );
        IssuedTokenPair first = service.issueTokenPair(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "subject",
                claims
        );

        service.refresh(first.refreshToken());

        assertThat(encoder.issuedClaims).hasSize(2);
        assertThat(encoder.lastClaims())
                .containsEntry("scope", "messenger:channel:create")
                .containsEntry("roles", List.of("tenant_admin"))
                .containsEntry("permissions", List.of("messenger:channel:delete:any"));
    }

    @Test
    void refresh_rejects_malformed_token_before_store_lookup() {
        InMemoryRefreshTokenStore store = new InMemoryRefreshTokenStore();
        RefreshTokenService service = service(new FakeJwtEncoder(), store);

        List.of("short", " ".repeat(43), "a".repeat(42), "a".repeat(44), "a".repeat(42) + "=")
                .forEach(rawToken -> assertThatThrownBy(() -> service.refresh(rawToken))
                        .isInstanceOf(InvalidRefreshTokenException.class)
                        .hasMessage("Invalid refresh token format"));

        assertThat(store.findByTokenHashCalls).isZero();
    }

    @Test
    void refresh_throws_when_refresh_token_is_expired() {
        InMemoryRefreshTokenStore store = new InMemoryRefreshTokenStore();
        RefreshTokenService service = service(new FakeJwtEncoder(), store);
        IssuedTokenPair tokenPair = service.issueTokenPair(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "subject",
                JwtAuthorityClaims.empty()
        );
        RefreshTokenRecord issued = store.singleRecord();
        store.replace(withExpiresAt(issued, Instant.now().minus(Duration.ofMinutes(1))));

        assertThatThrownBy(() -> service.refresh(tokenPair.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token is expired or revoked");
    }

    @Test
    void refresh_throws_when_refresh_token_not_found() {
        RefreshTokenService service = service(new FakeJwtEncoder(), new InMemoryRefreshTokenStore());

        assertThatThrownBy(() -> service.refresh(validUnknownRefreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token was not found");
    }

    @Test
    void refresh_revokes_preissued_token_best_effort_when_previous_token_was_already_used() {
        InMemoryRefreshTokenStore store = new InMemoryRefreshTokenStore();
        store.returnFalseOnNextRevoke = true;
        store.throwOnNextRevokeAfterConflict = true;
        RefreshTokenService service = service(new FakeJwtEncoder(), store);
        IssuedTokenPair first = service.issueTokenPair(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "subject",
                JwtAuthorityClaims.empty()
        );

        assertThatThrownBy(() -> service.refresh(first.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token was already used");

        assertThat(store.markUsedAndRevokeCalls).isEqualTo(1);
        assertThat(store.revokeCalls).isEqualTo(1);
        assertThat(store.revokeTokenFamilyCalls).isEqualTo(1);
    }

    @Test
    void revoke_is_idempotent_for_nonexistent_token() {
        RefreshTokenService service = service(new FakeJwtEncoder(), new InMemoryRefreshTokenStore());

        assertThatCode(() -> service.revoke(validUnknownRefreshToken()))
                .doesNotThrowAnyException();
    }

    @Test
    void revokeUserTokens_revokes_active_user_tokens() {
        InMemoryRefreshTokenStore store = new InMemoryRefreshTokenStore();
        RefreshTokenService service = service(new FakeJwtEncoder(), store);
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        IssuedTokenPair tokenPair = service.issueTokenPair(
                userId,
                tenantId,
                "subject",
                JwtAuthorityClaims.empty()
        );

        service.revokeUserTokens(tenantId, userId);

        assertThatThrownBy(() -> service.refresh(tokenPair.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    private static String validUnknownRefreshToken() {
        return "a".repeat(43);
    }

    private static RefreshTokenRecord withExpiresAt(RefreshTokenRecord record, Instant expiresAt) {
        return new RefreshTokenRecord(
                record.id(),
                record.tokenHash(),
                record.tenantId(),
                record.userId(),
                record.subject(),
                record.authorityClaims(),
                record.issuedAt(),
                record.usedAt(),
                expiresAt,
                record.revokedAt(),
                record.replacedByTokenId()
        );
    }

    private static RefreshTokenRecord withUsedAt(RefreshTokenRecord record, Instant usedAt) {
        return new RefreshTokenRecord(
                record.id(),
                record.tokenHash(),
                record.tenantId(),
                record.userId(),
                record.subject(),
                record.authorityClaims(),
                record.issuedAt(),
                usedAt,
                record.expiresAt(),
                record.revokedAt(),
                record.replacedByTokenId()
        );
    }

    private static IssuedTokenPair await(Future<IssuedTokenPair> future)
            throws InterruptedException, ExecutionException {
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new AssertionError("Timed out waiting for concurrent refresh", e);
        }
    }

    private RefreshTokenService service(FakeJwtEncoder encoder, InMemoryRefreshTokenStore store) {
        return service(encoder, store, new InMemoryRefreshTokenReplayCache());
    }

    private RefreshTokenService service(
            FakeJwtEncoder encoder,
            InMemoryRefreshTokenStore store,
            RefreshTokenReplayProperties replayProperties
    ) {
        return service(encoder, store, new InMemoryRefreshTokenReplayCache(), replayProperties);
    }

    private RefreshTokenService service(
            FakeJwtEncoder encoder,
            InMemoryRefreshTokenStore store,
            RefreshTokenReplayCache replayCache
    ) {
        return service(encoder, store, replayCache, replayProperties);
    }

    private RefreshTokenService service(
            FakeJwtEncoder encoder,
            InMemoryRefreshTokenStore store,
            RefreshTokenReplayCache replayCache,
            RefreshTokenReplayProperties replayProperties
    ) {
        return new RefreshTokenService(
                properties,
                replayProperties,
                provider(encoder),
                store,
                replayCache
        );
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private ObjectProvider<JwtEncoder> provider(JwtEncoder encoder) {
        return new ObjectProvider<>() {
            @Override
            public JwtEncoder getObject(Object... args) {
                return encoder;
            }

            @Override
            public JwtEncoder getIfAvailable() {
                return encoder;
            }

            @Override
            public JwtEncoder getIfUnique() {
                return encoder;
            }

            @Override
            public JwtEncoder getObject() {
                return encoder;
            }

            @Override
            public Iterator<JwtEncoder> iterator() {
                return List.of(encoder).iterator();
            }
        };
    }

    private static final class FakeJwtEncoder implements JwtEncoder {
        private int issued;
        private final List<Map<String, Object>> issuedClaims = new ArrayList<>();
        private final List<Instant> issuedExpiresAt = new ArrayList<>();

        @Override
        public Jwt encode(JwtEncoderParameters parameters) {
            issued++;
            Map<String, Object> claims = new HashMap<>(parameters.getClaims().getClaims());
            issuedClaims.add(claims);
            issuedExpiresAt.add(parameters.getClaims().getExpiresAt());
            return new Jwt(
                    "access-" + issued,
                    parameters.getClaims().getIssuedAt(),
                    parameters.getClaims().getExpiresAt(),
                    Map.of("alg", "HS256"),
                    claims
            );
        }

        private Map<String, Object> lastClaims() {
            return issuedClaims.get(issuedClaims.size() - 1);
        }

        private Instant lastExpiresAt() {
            return issuedExpiresAt.get(issuedExpiresAt.size() - 1);
        }
    }

    private static final class InMemoryRefreshTokenStore implements RefreshTokenStore {
        private final Map<String, RefreshTokenRecord> records = new HashMap<>();
        private int findByTokenHashCalls;
        private int markUsedAndRevokeCalls;
        private int revokeCalls;
        private int revokeTokenFamilyCalls;
        private boolean returnFalseOnNextRevoke;
        private boolean throwOnNextRevokeAfterConflict;
        private CyclicBarrier nextFindByTokenHashBarrier;
        private int nextFindByTokenHashBarrierRemaining;
        private CountDownLatch failedMarkCanReturn;

        @Override
        public synchronized void save(RefreshTokenRecord token) {
            records.put(token.tokenHash(), token);
        }

        private RefreshTokenRecord singleRecord() {
            return records.values().iterator().next();
        }

        private Optional<RefreshTokenRecord> findByRawToken(String rawToken) {
            return Optional.ofNullable(records.get(hash(rawToken)));
        }

        private synchronized void replace(RefreshTokenRecord token) {
            records.put(token.tokenHash(), token);
        }

        private synchronized int activeRefreshTokenCount() {
            return (int) records.values().stream()
                    .filter(record -> record.revokedAt() == null)
                    .filter(record -> record.usedAt() == null)
                    .count();
        }

        private synchronized void synchronizeNextFindByTokenHash(int parties) {
            nextFindByTokenHashBarrier = new CyclicBarrier(parties);
            nextFindByTokenHashBarrierRemaining = parties;
        }

        private synchronized void waitForReplayBeforeFailedMark(CountDownLatch latch) {
            failedMarkCanReturn = latch;
        }

        @Override
        public Optional<RefreshTokenRecord> findByTokenHash(String tokenHash) {
            CyclicBarrier barrier;
            Optional<RefreshTokenRecord> result;
            synchronized (this) {
                findByTokenHashCalls++;
                result = Optional.ofNullable(records.get(tokenHash));
                barrier = nextFindByTokenHashBarrier;
                if (barrier != null && --nextFindByTokenHashBarrierRemaining == 0) {
                    nextFindByTokenHashBarrier = null;
                }
            }
            if (barrier != null) {
                awaitBarrier(barrier);
            }
            return result;
        }

        @Override
        public boolean markUsedAndRevoke(UUID tokenId, UUID replacedByTokenId, Instant usedAt) {
            CountDownLatch latch = null;
            synchronized (this) {
                markUsedAndRevokeCalls++;
                if (returnFalseOnNextRevoke) {
                    returnFalseOnNextRevoke = false;
                    return false;
                }
                Optional<RefreshTokenRecord> current = records.values().stream()
                        .filter(record -> record.id().equals(tokenId))
                        .filter(record -> record.usedAt() == null)
                        .filter(record -> record.revokedAt() == null)
                        .findFirst();
                current.ifPresent(record -> records.put(
                        record.tokenHash(),
                        new RefreshTokenRecord(
                                record.id(),
                                record.tokenHash(),
                                record.tenantId(),
                                record.userId(),
                                record.subject(),
                                record.authorityClaims(),
                                record.issuedAt(),
                                usedAt,
                                record.expiresAt(),
                                usedAt,
                                replacedByTokenId
                        )
                ));
                if (current.isPresent()) {
                    return true;
                }
                latch = failedMarkCanReturn;
            }
            if (latch != null) {
                awaitLatch(latch);
            }
            return false;
        }

        @Override
        public synchronized boolean revoke(UUID tokenId, UUID replacedByTokenId, Instant revokedAt) {
            revokeCalls++;
            if (throwOnNextRevokeAfterConflict) {
                throwOnNextRevokeAfterConflict = false;
                throw new IllegalStateException("cleanup failed");
            }
            Optional<RefreshTokenRecord> current = records.values().stream()
                    .filter(record -> record.id().equals(tokenId))
                    .filter(record -> record.revokedAt() == null)
                    .findFirst();
            current.ifPresent(record -> records.put(
                            record.tokenHash(),
                            new RefreshTokenRecord(
                                    record.id(),
                                    record.tokenHash(),
                                    record.tenantId(),
                                    record.userId(),
                                    record.subject(),
                                    record.authorityClaims(),
                                    record.issuedAt(),
                                    record.usedAt(),
                                    record.expiresAt(),
                                    revokedAt,
                                    replacedByTokenId
                            )
                    ));
            return current.isPresent();
        }

        @Override
        public synchronized void revokeTokenFamily(UUID rootTokenId, Instant revokedAt) {
            revokeTokenFamilyCalls++;
            Set<UUID> visited = new HashSet<>();
            UUID currentId = rootTokenId;
            while (currentId != null && visited.add(currentId)) {
                Optional<RefreshTokenRecord> current = findById(currentId);
                if (current.isEmpty()) {
                    return;
                }
                RefreshTokenRecord record = current.get();
                if (record.revokedAt() == null) {
                    replace(new RefreshTokenRecord(
                            record.id(),
                            record.tokenHash(),
                            record.tenantId(),
                            record.userId(),
                            record.subject(),
                            record.authorityClaims(),
                            record.issuedAt(),
                            record.usedAt(),
                            record.expiresAt(),
                            revokedAt,
                            record.replacedByTokenId()
                    ));
                }
                currentId = record.replacedByTokenId();
            }
        }

        @Override
        public synchronized void revokeUserTokens(UUID tenantId, UUID userId, Instant revokedAt) {
            List<RefreshTokenRecord> userTokens = records.values().stream()
                    .filter(record -> record.tenantId().equals(tenantId))
                    .filter(record -> record.userId().equals(userId))
                    .toList();
            userTokens.forEach(record -> revoke(record.id(), null, revokedAt));
        }

        private Optional<RefreshTokenRecord> findById(UUID tokenId) {
            return records.values().stream()
                    .filter(record -> record.id().equals(tokenId))
                    .findFirst();
        }

        private void awaitBarrier(CyclicBarrier barrier) {
            try {
                barrier.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for refresh race barrier", e);
            } catch (BrokenBarrierException | java.util.concurrent.TimeoutException e) {
                throw new AssertionError("Timed out waiting for refresh race barrier", e);
            }
        }

        private void awaitLatch(CountDownLatch latch) {
            try {
                if (!latch.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("Timed out waiting for refresh replay cache save");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for refresh replay cache save", e);
            }
        }
    }

    private static final class InMemoryRefreshTokenReplayCache implements RefreshTokenReplayCache {
        private final Map<String, IssuedTokenPair> requestCache = new HashMap<>();
        private final Map<UUID, IssuedTokenPair> usedTokenCache = new HashMap<>();
        private CountDownLatch usedTokenReplaySaveSignal;

        private void signalUsedTokenReplaySave(CountDownLatch latch) {
            usedTokenReplaySaveSignal = latch;
        }

        @Override
        public Optional<IssuedTokenPair> findByRequest(String refreshTokenHash, String requestId) {
            return Optional.ofNullable(requestCache.get(requestKey(refreshTokenHash, requestId)));
        }

        @Override
        public void saveByRequest(String refreshTokenHash, String requestId, IssuedTokenPair tokenPair, Duration ttl) {
            if (!ttl.isZero() && !ttl.isNegative()) {
                requestCache.put(requestKey(refreshTokenHash, requestId), tokenPair);
            }
        }

        @Override
        public Optional<IssuedTokenPair> findByUsedToken(UUID tokenId) {
            return Optional.ofNullable(usedTokenCache.get(tokenId));
        }

        @Override
        public void saveByUsedToken(UUID tokenId, IssuedTokenPair tokenPair, Duration ttl) {
            if (!ttl.isZero() && !ttl.isNegative()) {
                usedTokenCache.put(tokenId, tokenPair);
            }
            if (usedTokenReplaySaveSignal != null) {
                usedTokenReplaySaveSignal.countDown();
            }
        }

        private String requestKey(String refreshTokenHash, String requestId) {
            return requestId + ":" + refreshTokenHash;
        }
    }

    private static final class NoopRefreshTokenReplayCache implements RefreshTokenReplayCache {

        @Override
        public Optional<IssuedTokenPair> findByRequest(String refreshTokenHash, String requestId) {
            return Optional.empty();
        }

        @Override
        public void saveByRequest(String refreshTokenHash, String requestId, IssuedTokenPair tokenPair, Duration ttl) {
        }

        @Override
        public Optional<IssuedTokenPair> findByUsedToken(UUID tokenId) {
            return Optional.empty();
        }

        @Override
        public void saveByUsedToken(UUID tokenId, IssuedTokenPair tokenPair, Duration ttl) {
        }
    }
}
