package com.polarishb.pabal.security.token;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcRefreshTokenStore implements RefreshTokenStore {

    private static final String INSERT_TOKEN = """
            INSERT INTO security_refresh_token (
                id,
                token_hash,
                tenant_id,
                user_id,
                subject,
                authority_claims,
                issued_at,
                used_at,
                expires_at,
                revoked_at,
                replaced_by_token_id
            )
            VALUES (
                :id,
                :tokenHash,
                :tenantId,
                :userId,
                :subject,
                :authorityClaims,
                :issuedAt,
                :usedAt,
                :expiresAt,
                :revokedAt,
                :replacedByTokenId
            )
            """;

    private static final String FIND_BY_HASH = """
            SELECT
                id,
                token_hash,
                tenant_id,
                user_id,
                subject,
                authority_claims,
                issued_at,
                used_at,
                expires_at,
                revoked_at,
                replaced_by_token_id
            FROM security_refresh_token
            WHERE token_hash = :tokenHash
            """;

    private static final String MARK_USED_AND_REVOKE_TOKEN = """
            UPDATE security_refresh_token
            SET used_at = :usedAt,
                revoked_at = :usedAt,
                replaced_by_token_id = :replacedByTokenId
            WHERE id = :id
              AND used_at IS NULL
              AND revoked_at IS NULL
            """;

    private static final String REVOKE_TOKEN = """
            UPDATE security_refresh_token
            SET revoked_at = :revokedAt,
                replaced_by_token_id = :replacedByTokenId
            WHERE id = :id
              AND revoked_at IS NULL
            """;

    private static final String REVOKE_USER_TOKENS = """
            UPDATE security_refresh_token
            SET revoked_at = :revokedAt
            WHERE tenant_id = :tenantId
              AND user_id = :userId
              AND revoked_at IS NULL
            """;

    private static final String REVOKE_TOKEN_FAMILY = """
            WITH RECURSIVE token_family AS (
                SELECT id, replaced_by_token_id
                FROM security_refresh_token
                WHERE id = :rootTokenId

                UNION

                SELECT next_token.id, next_token.replaced_by_token_id
                FROM security_refresh_token next_token
                JOIN token_family current_token ON next_token.id = current_token.replaced_by_token_id
            )
            UPDATE security_refresh_token token
            SET revoked_at = :revokedAt
            WHERE token.id IN (SELECT id FROM token_family)
              AND token.revoked_at IS NULL
            """;

    private final JsonMapper jsonMapper;
    private final JdbcClient jdbcClient;

    public JdbcRefreshTokenStore(DataSource dataSource, JsonMapper jsonMapper) {
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper must not be null");
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    @Override
    public void save(RefreshTokenRecord token) {
        jdbcClient.sql(INSERT_TOKEN)
                .param("id", token.id())
                .param("tokenHash", token.tokenHash())
                .param("tenantId", token.tenantId())
                .param("userId", token.userId())
                .param("subject", token.subject())
                .param("authorityClaims", writeClaims(token.authorityClaims()))
                .param("issuedAt", toTimestamp(token.issuedAt()))
                .param("usedAt", toTimestamp(token.usedAt()))
                .param("expiresAt", toTimestamp(token.expiresAt()))
                .param("revokedAt", toTimestamp(token.revokedAt()))
                .param("replacedByTokenId", token.replacedByTokenId())
                .update();
    }

    @Override
    public Optional<RefreshTokenRecord> findByTokenHash(String tokenHash) {
        return jdbcClient.sql(FIND_BY_HASH)
                .param("tokenHash", tokenHash)
                .query((rs, rowNum) -> new RefreshTokenRecord(
                        rs.getObject("id", UUID.class),
                        rs.getString("token_hash"),
                        rs.getObject("tenant_id", UUID.class),
                        rs.getObject("user_id", UUID.class),
                        rs.getString("subject"),
                        readClaims(rs.getString("authority_claims")),
                        toInstant(rs.getTimestamp("issued_at")),
                        toInstant(rs.getTimestamp("used_at")),
                        toInstant(rs.getTimestamp("expires_at")),
                        toInstant(rs.getTimestamp("revoked_at")),
                        rs.getObject("replaced_by_token_id", UUID.class)
                ))
                .optional();
    }

    @Override
    public boolean markUsedAndRevoke(UUID tokenId, UUID replacedByTokenId, Instant usedAt) {
        int updated = jdbcClient.sql(MARK_USED_AND_REVOKE_TOKEN)
                .param("id", tokenId)
                .param("replacedByTokenId", replacedByTokenId)
                .param("usedAt", toTimestamp(usedAt))
                .update();
        return updated == 1;
    }

    @Override
    public boolean revoke(UUID tokenId, UUID replacedByTokenId, Instant revokedAt) {
        int updated = jdbcClient.sql(REVOKE_TOKEN)
                .param("id", tokenId)
                .param("replacedByTokenId", replacedByTokenId)
                .param("revokedAt", toTimestamp(revokedAt))
                .update();
        return updated == 1;
    }

    @Override
    public void revokeTokenFamily(UUID rootTokenId, Instant revokedAt) {
        jdbcClient.sql(REVOKE_TOKEN_FAMILY)
                .param("rootTokenId", rootTokenId)
                .param("revokedAt", toTimestamp(revokedAt))
                .update();
    }

    @Override
    public void revokeUserTokens(UUID tenantId, UUID userId, Instant revokedAt) {
        jdbcClient.sql(REVOKE_USER_TOKENS)
                .param("tenantId", tenantId)
                .param("userId", userId)
                .param("revokedAt", toTimestamp(revokedAt))
                .update();
    }

    private String writeClaims(JwtAuthorityClaims claims) {
        try {
            return jsonMapper.writeValueAsString(claims);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Failed to serialize refresh token authority claims", ex);
        }
    }

    private JwtAuthorityClaims readClaims(String value) {
        try {
            return jsonMapper.readValue(value, JwtAuthorityClaims.class);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Failed to deserialize refresh token authority claims", ex);
        }
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
