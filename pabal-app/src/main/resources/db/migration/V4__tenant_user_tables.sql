CREATE TABLE IF NOT EXISTS tenant_user (
    id uuid PRIMARY KEY,
    tenant_id uuid NOT NULL,
    name varchar(100) NOT NULL,
    status varchar(20) NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,

    CONSTRAINT uq_tenant_user_tenant_id_id UNIQUE (tenant_id, id),
    CONSTRAINT chk_tenant_user_status
        CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT chk_tenant_user_name_not_blank
        CHECK (length(trim(name)) > 0)
);

CREATE INDEX IF NOT EXISTS idx_tenant_user_tenant_status
    ON tenant_user (tenant_id, status);
