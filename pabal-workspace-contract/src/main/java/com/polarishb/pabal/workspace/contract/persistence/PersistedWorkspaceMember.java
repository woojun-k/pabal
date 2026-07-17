package com.polarishb.pabal.workspace.contract.persistence;

import com.polarishb.pabal.workspace.domain.model.WorkspaceMember;

import java.util.Objects;

public record PersistedWorkspaceMember(
        WorkspaceMember member,
        WorkspaceMemberState state
) {
    public PersistedWorkspaceMember {
        Objects.requireNonNull(member);
        Objects.requireNonNull(state);
        if (!Objects.equals(member.getId(), state.id())) {
            throw new IllegalArgumentException("member id must match persisted state id");
        }
        if (!Objects.equals(member.getTenantId(), state.tenantId())) {
            throw new IllegalArgumentException("member tenantId must match persisted state tenantId");
        }
        if (!Objects.equals(member.getWorkspaceId(), state.workspaceId())) {
            throw new IllegalArgumentException("member workspaceId must match persisted state workspaceId");
        }
        if (!Objects.equals(member.getUserId(), state.userId())) {
            throw new IllegalArgumentException("member userId must match persisted state userId");
        }
    }

    /**
     * 불변 도메인 전이 결과(next)를 조회 당시 persistence 기준점(state)에 다시 묶는다.
     *
     * <p>state는 그대로 보존된다. state가 optimistic lock 검증(version)과 row 식별(id)의
     * 기준점이기 때문이다. next는 저장하려는 변경 후 도메인 상태다. 두 값이 같은 aggregate를
     * 가리키는지 id/tenantId/workspaceId/userId로 검증해 잘못된 aggregate가 끼워지는 배선
     * 오류를 막는다.
     */
    public PersistedWorkspaceMember withMember(WorkspaceMember next) {
        Objects.requireNonNull(next);
        if (!Objects.equals(next.getId(), state.id())) {
            throw new IllegalArgumentException("member id must match persisted state id");
        }
        if (!Objects.equals(next.getTenantId(), state.tenantId())) {
            throw new IllegalArgumentException("member tenantId must match persisted state tenantId");
        }
        if (!Objects.equals(next.getWorkspaceId(), state.workspaceId())) {
            throw new IllegalArgumentException("member workspaceId must match persisted state workspaceId");
        }
        if (!Objects.equals(next.getUserId(), state.userId())) {
            throw new IllegalArgumentException("member userId must match persisted state userId");
        }
        return new PersistedWorkspaceMember(next, state);
    }
}
