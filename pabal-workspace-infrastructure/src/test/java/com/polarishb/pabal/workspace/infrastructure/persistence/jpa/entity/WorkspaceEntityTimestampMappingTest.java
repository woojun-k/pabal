package com.polarishb.pabal.workspace.infrastructure.persistence.jpa.entity;

import com.polarishb.pabal.workspace.contract.persistence.WorkspaceMemberState;
import com.polarishb.pabal.workspace.contract.persistence.WorkspaceState;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceMemberStatus;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceRole;
import com.polarishb.pabal.workspace.domain.model.type.WorkspaceStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WorkspaceEntityTimestampMappingTest {

    private static final Instant CREATED_AT = Instant.parse("2026-06-19T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-06-19T01:00:00Z");

    @Test
    void workspace_fromNewState_maps_contract_timestamps_to_base_entity_fields() {
        WorkspaceState state = new WorkspaceState(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Engineering",
                WorkspaceStatus.ACTIVE,
                UUID.randomUUID(),
                CREATED_AT,
                UPDATED_AT,
                null
        );

        WorkspaceEntity entity = WorkspaceEntity.fromNewState(state);

        assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(entity.getUpdatedAt()).isEqualTo(UPDATED_AT);
        assertThat(entity.toState().createdAt()).isEqualTo(CREATED_AT);
        assertThat(entity.toState().updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void workspaceMember_fromNewState_maps_contract_timestamps_to_base_entity_fields() {
        WorkspaceMemberState state = new WorkspaceMemberState(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                WorkspaceRole.OWNER,
                WorkspaceMemberStatus.ACTIVE,
                CREATED_AT,
                null,
                CREATED_AT,
                UPDATED_AT,
                null
        );

        WorkspaceMemberEntity entity = WorkspaceMemberEntity.fromNewState(state);

        assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(entity.getUpdatedAt()).isEqualTo(UPDATED_AT);
        assertThat(entity.toState().createdAt()).isEqualTo(CREATED_AT);
        assertThat(entity.toState().updatedAt()).isEqualTo(UPDATED_AT);
    }
}
