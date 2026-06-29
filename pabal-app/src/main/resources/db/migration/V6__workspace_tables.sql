CREATE TABLE IF NOT EXISTS workspace (
    id uuid PRIMARY KEY,
    tenant_id uuid NOT NULL,
    name varchar(100) NOT NULL,
    status varchar(20) NOT NULL,
    created_by uuid NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,

    CONSTRAINT uq_workspace_tenant_id_id UNIQUE (tenant_id, id),
    CONSTRAINT chk_workspace_status
        CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT chk_workspace_name_not_blank
        CHECK (length(trim(name)) > 0)
);

CREATE INDEX IF NOT EXISTS idx_workspace_tenant_status
    ON workspace (tenant_id, status);

CREATE TABLE IF NOT EXISTS workspace_member (
    id uuid PRIMARY KEY,
    tenant_id uuid NOT NULL,
    workspace_id uuid NOT NULL,
    user_id uuid NOT NULL,
    role varchar(20) NOT NULL,
    status varchar(20) NOT NULL,
    joined_at timestamptz NOT NULL,
    left_at timestamptz,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,

    CONSTRAINT uq_workspace_member_tenant_workspace_user UNIQUE (tenant_id, workspace_id, user_id),
    CONSTRAINT fk_workspace_member_workspace
        FOREIGN KEY (tenant_id, workspace_id) REFERENCES workspace (tenant_id, id),
    CONSTRAINT chk_workspace_member_role
        CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER')),
    CONSTRAINT chk_workspace_member_status
        CHECK (status IN ('ACTIVE', 'LEFT')),
    CONSTRAINT chk_workspace_member_left_at
        CHECK ((status = 'LEFT' AND left_at IS NOT NULL) OR (status = 'ACTIVE' AND left_at IS NULL))
);

CREATE INDEX IF NOT EXISTS idx_workspace_member_active_lookup
    ON workspace_member (tenant_id, workspace_id, user_id, status);
