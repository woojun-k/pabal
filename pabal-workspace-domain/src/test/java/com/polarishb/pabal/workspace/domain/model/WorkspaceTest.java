package com.polarishb.pabal.workspace.domain.model;

import com.polarishb.pabal.workspace.domain.model.snapshot.WorkspaceSnapshot;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceStatus;
import com.polarishb.pabal.workspace.domain.model.vo.WorkspaceName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WorkspaceTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CREATED_BY = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Instant CREATED_AT = Instant.parse("2026-07-02T01:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-02T01:00:01Z");

    @Test
    void create_returns_an_active_workspace() {
        Workspace workspace = Workspace.create(TENANT_ID, "Engineering", CREATED_BY, CREATED_AT);

        assertThat(workspace.isActive()).isTrue();
        assertThat(workspace.snapshot().status()).isEqualTo(WorkspaceStatus.ACTIVE);
    }

    @Test
    void isActive_returns_false_for_reconstituted_non_active_workspace() {
        Workspace workspace = Workspace.reconstitute(new WorkspaceSnapshot(
                ID,
                TENANT_ID,
                new WorkspaceName("Archived"),
                WorkspaceStatus.ARCHIVED,
                CREATED_BY,
                CREATED_AT,
                UPDATED_AT
        ));

        assertThat(workspace.isActive()).isFalse();
    }
}
