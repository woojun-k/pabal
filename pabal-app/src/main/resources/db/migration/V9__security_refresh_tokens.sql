CREATE TABLE IF NOT EXISTS security_refresh_token (
    id uuid PRIMARY KEY,
    token_hash varchar(128) NOT NULL,
    tenant_id uuid NOT NULL,
    user_id uuid NOT NULL,
    subject varchar(255) NOT NULL,
    authority_claims text NOT NULL,
    issued_at timestamptz NOT NULL,
    used_at timestamptz,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    replaced_by_token_id uuid,

    CONSTRAINT uq_security_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_security_refresh_token_replacement
        FOREIGN KEY (replaced_by_token_id) REFERENCES security_refresh_token (id),
    CONSTRAINT chk_security_refresh_token_hash_not_blank
        CHECK (length(trim(token_hash)) > 0),
    CONSTRAINT chk_security_refresh_token_subject_not_blank
        CHECK (length(trim(subject)) > 0),
    CONSTRAINT chk_security_refresh_token_authority_claims_not_blank
        CHECK (length(trim(authority_claims)) > 0),
    CONSTRAINT chk_security_refresh_token_expires_after_issued
        CHECK (expires_at > issued_at),
    CONSTRAINT chk_security_refresh_token_used_after_issued
        CHECK (used_at IS NULL OR used_at >= issued_at),
    CONSTRAINT chk_security_refresh_token_revoked_after_issued
        CHECK (revoked_at IS NULL OR revoked_at >= issued_at)
);

CREATE INDEX IF NOT EXISTS idx_security_refresh_token_active_hash
    ON security_refresh_token (token_hash)
    WHERE revoked_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_security_refresh_token_user_active
    ON security_refresh_token (tenant_id, user_id, expires_at)
    WHERE revoked_at IS NULL;
