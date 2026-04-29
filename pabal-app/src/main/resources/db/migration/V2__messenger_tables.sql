-- =====================================================================
-- Messenger Tables
-- =====================================================================
-- 대상 JPA Entity
--  - ChatRoomEntity          -> chat_room
--  - ChatRoomMemberEntity    -> chat_room_member
--  - DirectChatMappingEntity -> direct_chat_mapping
--  - MessageEntity           -> message
--
-- 설계 원칙
--  - JPA는 객체-테이블 매핑과 validate만 담당한다.
--  - DB 구조, unique 제약, check 제약, index는 Flyway migration이 관리한다.
--  - 애플리케이션 레벨 검증은 UX/빠른 실패용이고,
--    동시성 race condition의 최종 방어는 DB 제약이다.
--  - 멀티테넌시 데이터 오염 방지를 위해 tenant_id를 FK/UNIQUE 제약에 포함한다.
-- =====================================================================


-- =====================================================================
-- Table: chat_room
--  - 채팅방 공통 메타데이터 저장
--  - DIRECT / GROUP / CHANNEL 타입을 하나의 테이블에서 관리
--  - CHANNEL 이름 중복은 partial unique index로 최종 보장
-- =====================================================================
CREATE TABLE IF NOT EXISTS chat_room (
    id                    UUID          PRIMARY KEY DEFAULT uuidv7(),

    type                  VARCHAR(30)   NOT NULL,
    name                  VARCHAR(255),
    created_by            UUID          NOT NULL,
    tenant_id             UUID          NOT NULL,

    workspace_id          UUID,
    is_private            BOOLEAN       NOT NULL DEFAULT FALSE,
    description           VARCHAR(255),

    status                VARCHAR(30)   NOT NULL,
    scheduled_deletion_at TIMESTAMPTZ,

    last_message_id       UUID,
    last_message_sequence BIGINT        NOT NULL DEFAULT 0,
    last_message_at       TIMESTAMPTZ,

    version               BIGINT        NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ   NOT NULL,
    updated_at            TIMESTAMPTZ   NOT NULL,
    deleted_at            TIMESTAMPTZ,

    CONSTRAINT uq_chat_room_tenant_id_id
    UNIQUE (tenant_id, id),

    CONSTRAINT chk_chat_room_type
    CHECK (type IN ('DIRECT', 'GROUP', 'CHANNEL')),

    CONSTRAINT chk_chat_room_status
    CHECK (status IN ('ACTIVE', 'PENDING_DELETION', 'DELETED')),

    CONSTRAINT chk_chat_room_channel_requires_workspace
    CHECK (type <> 'CHANNEL' OR workspace_id IS NOT NULL),

    CONSTRAINT chk_chat_room_channel_requires_name
    CHECK (type <> 'CHANNEL' OR name IS NOT NULL),

    CONSTRAINT chk_chat_room_direct_name_absent
    CHECK (type <> 'DIRECT' OR name IS NULL),

    CONSTRAINT chk_chat_room_last_message_sequence_non_negative
    CHECK (last_message_sequence >= 0),

    CONSTRAINT chk_chat_room_deleted_consistency
    CHECK (
        (status = 'DELETED' AND deleted_at IS NOT NULL)
    OR
        (status <> 'DELETED' AND deleted_at IS NULL)
    )
);

-- CHANNEL 이름 중복 방지
--  - 같은 tenant + workspace 안에서 살아있는 CHANNEL name은 하나만 허용
--  - DIRECT / GROUP에는 적용하지 않는다.
--  - lower(name)을 사용하여 대소문자만 다른 중복 채널명을 차단한다.
CREATE UNIQUE INDEX IF NOT EXISTS uq_chat_room_channel_name_alive
    ON chat_room (tenant_id, workspace_id, lower(name))
    WHERE type = 'CHANNEL'
    AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_chat_room_channel_lookup_alive
    ON chat_room (tenant_id, workspace_id, type, lower(name))
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_chat_room_tenant_id_id_alive
    ON chat_room (tenant_id, id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_chat_room_last_message_at_alive
    ON chat_room (tenant_id, last_message_at DESC, created_at DESC)
    WHERE deleted_at IS NULL;


-- =====================================================================
-- Table: chat_room_member
--  - 사용자와 채팅방 membership 관계 저장
--  - left_at이 NULL이면 현재 활성 membership으로 판단한다.
-- =====================================================================
CREATE TABLE IF NOT EXISTS chat_room_member (
    id                   UUID          PRIMARY KEY DEFAULT uuidv7(),

    tenant_id            UUID          NOT NULL,
    chat_room_id         UUID          NOT NULL,
    user_id              UUID          NOT NULL,

    last_read_message_id UUID,
    last_read_sequence   BIGINT,
    last_read_at         TIMESTAMPTZ,

    joined_at            TIMESTAMPTZ   NOT NULL,
    left_at              TIMESTAMPTZ,

    version              BIGINT        NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ   NOT NULL,
    updated_at           TIMESTAMPTZ   NOT NULL,
    deleted_at           TIMESTAMPTZ,

    CONSTRAINT fk_chat_room_member_room
    FOREIGN KEY (tenant_id, chat_room_id)
    REFERENCES chat_room (tenant_id, id)
    ON DELETE RESTRICT,

    CONSTRAINT uq_chat_room_member
    UNIQUE (tenant_id, chat_room_id, user_id),

    CONSTRAINT uq_chat_room_member_tenant_room_user
    UNIQUE (tenant_id, chat_room_id, user_id),

    CONSTRAINT chk_chat_room_member_last_read_sequence_non_negative
    CHECK (last_read_sequence IS NULL OR last_read_sequence >= 0),

    CONSTRAINT chk_chat_room_member_left_after_join
    CHECK (left_at IS NULL OR left_at >= joined_at)
);

CREATE INDEX IF NOT EXISTS idx_chat_room_member_user_active
    ON chat_room_member (tenant_id, user_id, chat_room_id)
    WHERE left_at IS NULL
    AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_chat_room_member_room_active
    ON chat_room_member (tenant_id, chat_room_id, user_id)
    WHERE left_at IS NULL
    AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_chat_room_member_room_left_at
    ON chat_room_member (tenant_id, chat_room_id, left_at);


-- =====================================================================
-- Table: direct_chat_mapping
--  - 1:1 direct room의 participant pair -> chat_room_id 매핑
--  - user_id_min / user_id_max 정렬 저장으로 A-B와 B-A를 동일 pair로 취급
-- =====================================================================
CREATE TABLE IF NOT EXISTS direct_chat_mapping (
    id           UUID        PRIMARY KEY DEFAULT uuidv7(),

    tenant_id    UUID        NOT NULL,
    chat_room_id UUID        NOT NULL,
    user_id_min  UUID        NOT NULL,
    user_id_max  UUID        NOT NULL,

    version      BIGINT      NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_direct_chat_mapping_room
    FOREIGN KEY (tenant_id, chat_room_id)
    REFERENCES chat_room (tenant_id, id)
    ON DELETE RESTRICT,

    CONSTRAINT uq_direct_chat_mapping
    UNIQUE (tenant_id, user_id_min, user_id_max),

    CONSTRAINT uq_direct_chat_mapping_room
    UNIQUE (tenant_id, chat_room_id),

    CONSTRAINT chk_direct_chat_mapping_distinct_users
    CHECK (user_id_min <> user_id_max),

    CONSTRAINT chk_direct_chat_mapping_user_order
    CHECK (user_id_min < user_id_max)
);

CREATE INDEX IF NOT EXISTS idx_direct_chat_mapping_room
    ON direct_chat_mapping (tenant_id, chat_room_id);


-- =====================================================================
-- Table: message
--  - 채팅 메시지 저장
--  - room sequence는 room 내부 메시지 순서와 unread count 기준으로 사용
--  - client_message_id는 클라이언트 재시도/idempotency 보장용
-- =====================================================================
CREATE TABLE IF NOT EXISTS message (
    id                  UUID           PRIMARY KEY DEFAULT uuidv7(),

    tenant_id           UUID           NOT NULL,
    chat_room_id        UUID           NOT NULL,
    sender_id           UUID           NOT NULL,
    client_message_id   UUID           NOT NULL,

    sequence            BIGINT         NOT NULL,
    type                VARCHAR(30)    NOT NULL,
    content             TEXT           NOT NULL,
    status              VARCHAR(30)    NOT NULL,
    reply_to_message_id UUID,

    version             BIGINT         NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ    NOT NULL,
    updated_at          TIMESTAMPTZ    NOT NULL,
    deleted_at          TIMESTAMPTZ,

    CONSTRAINT fk_message_room
    FOREIGN KEY (tenant_id, chat_room_id)
    REFERENCES chat_room (tenant_id, id)
    ON DELETE RESTRICT,

    CONSTRAINT fk_message_reply_to
    FOREIGN KEY (tenant_id, chat_room_id, reply_to_message_id)
    REFERENCES message (tenant_id, chat_room_id, id)
    ON DELETE RESTRICT,

    CONSTRAINT uq_message_tenant_room_id
    UNIQUE (tenant_id, chat_room_id, id),

    CONSTRAINT uq_message_client_id
    UNIQUE (tenant_id, chat_room_id, sender_id, client_message_id),

    CONSTRAINT uq_message_room_sequence
    UNIQUE (tenant_id, chat_room_id, sequence),

    CONSTRAINT chk_message_type
    CHECK (type IN ('USER', 'SYSTEM')),

    CONSTRAINT chk_message_status
    CHECK (status IN ('ACTIVE', 'DELETED', 'EDITED')),

    CONSTRAINT chk_message_content_length
    CHECK (char_length(content) BETWEEN 1 AND 5000),

    CONSTRAINT chk_message_sequence_positive
    CHECK (sequence > 0),

    CONSTRAINT chk_message_deleted_consistency
    CHECK (
        (status = 'DELETED' AND deleted_at IS NOT NULL)
    OR
        (status <> 'DELETED' AND deleted_at IS NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_message_room_sequence_desc
    ON message (tenant_id, chat_room_id, sequence DESC);

CREATE INDEX IF NOT EXISTS idx_message_room_created
    ON message (tenant_id, chat_room_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_message_unread_count
    ON message (tenant_id, chat_room_id, sequence, sender_id, status);

CREATE INDEX IF NOT EXISTS idx_message_client_lookup
    ON message (tenant_id, chat_room_id, sender_id, client_message_id);

CREATE INDEX IF NOT EXISTS idx_message_reply_to
    ON message (tenant_id, chat_room_id, reply_to_message_id)
    WHERE reply_to_message_id IS NOT NULL;


-- =====================================================================
-- Cross-table FK constraints that require message table to exist first
-- =====================================================================

-- chat_room.last_message_id가 같은 tenant + 같은 room의 message만 가리키도록 보장한다.
ALTER TABLE chat_room
    ADD CONSTRAINT fk_chat_room_last_message
        FOREIGN KEY (tenant_id, id, last_message_id)
            REFERENCES message (tenant_id, chat_room_id, id)
            ON DELETE RESTRICT;

-- chat_room_member.last_read_message_id가 같은 tenant + 같은 room의 message만 가리키도록 보장한다.
ALTER TABLE chat_room_member
    ADD CONSTRAINT fk_chat_room_member_last_read_message
        FOREIGN KEY (tenant_id, chat_room_id, last_read_message_id)
            REFERENCES message (tenant_id, chat_room_id, id)
            ON DELETE RESTRICT;