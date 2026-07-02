package com.polarishb.pabal.workspace.domain.model.snapshot;

import com.polarishb.pabal.workspace.domain.model.type.WorkspaceMemberStatus;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class WorkspaceMemberSnapshotTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID WORKSPACE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant JOINED_AT = Instant.parse("2026-07-02T01:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-07-02T01:00:01Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-02T01:00:02Z");
    private static final Instant LEFT_AT = Instant.parse("2026-07-02T02:00:00Z");

    @Test
    void active_snapshot_rejects_leftAt() {
        Throwable thrown = catchThrowable(() -> snapshot(WorkspaceMemberStatus.ACTIVE, LEFT_AT));

        assertThat(thrown).isNotNull();
    }

    @Test
    void left_snapshot_rejects_missing_leftAt() {
        Throwable thrown = catchThrowable(() -> snapshot(WorkspaceMemberStatus.LEFT, null));

        assertThat(thrown).isNotNull();
    }

    private static WorkspaceMemberSnapshot snapshot(WorkspaceMemberStatus status, Instant leftAt) {
        return new WorkspaceMemberSnapshot(
                ID,
                TENANT_ID,
                WORKSPACE_ID,
                USER_ID,
                WorkspaceRole.MEMBER,
                status,
                JOINED_AT,
                leftAt,
                CREATED_AT,
                UPDATED_AT
        );
    }
}
