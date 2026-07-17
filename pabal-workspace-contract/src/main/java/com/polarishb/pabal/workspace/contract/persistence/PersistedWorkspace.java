package com.polarishb.pabal.workspace.contract.persistence;

import com.polarishb.pabal.workspace.domain.model.Workspace;

import java.util.Objects;

public record PersistedWorkspace(
        Workspace workspace,
        WorkspaceState state
) {
    public PersistedWorkspace {
        Objects.requireNonNull(workspace);
        Objects.requireNonNull(state);
        if (!Objects.equals(workspace.getId(), state.id())) {
            throw new IllegalArgumentException("workspace id must match persisted state id");
        }
        if (!Objects.equals(workspace.getTenantId(), state.tenantId())) {
            throw new IllegalArgumentException("workspace tenantId must match persisted state tenantId");
        }
    }

    /**
     * 불변 도메인 전이 결과(next)를 조회 당시 persistence 기준점(state)에 다시 묶는다.
     *
     * <p>state는 그대로 보존된다. state가 optimistic lock 검증(version)과 row 식별(id)의
     * 기준점이기 때문이다. next는 저장하려는 변경 후 도메인 상태다. 두 값이 같은 aggregate를
     * 가리키는지 id/tenantId로 검증해 잘못된 aggregate가 끼워지는 배선 오류를 막는다.
     */
    public PersistedWorkspace withWorkspace(Workspace next) {
        Objects.requireNonNull(next);
        if (!Objects.equals(next.getId(), state.id())) {
            throw new IllegalArgumentException("workspace id must match persisted state id");
        }
        if (!Objects.equals(next.getTenantId(), state.tenantId())) {
            throw new IllegalArgumentException("workspace tenantId must match persisted state tenantId");
        }
        return new PersistedWorkspace(next, state);
    }
}
