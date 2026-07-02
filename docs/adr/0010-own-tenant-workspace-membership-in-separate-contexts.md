# ADR-0010: Tenant와 Workspace membership은 별도 bounded context가 소유한다

## Status

Accepted

## Context

Messenger는 메시지 전송, room membership, channel 생성에서 tenant user와 workspace member 여부를 확인해야 한다. 이전 임시 구현은 workspace participant 조회를 현재 인증 principal 중심으로 축소했고, messenger DB의 `chat_room_member` 상태와 user/workspace source of truth가 불일치할 때 어떤 데이터를 신뢰해야 하는지 명확하지 않았다.

Pabal은 현재 MSA가 아니라 단일 배포 멀티모듈 모놀리스다. 그래도 tenant, workspace, user, messenger의 데이터 소유권을 코드 구조에서 분리해야 나중에 MSA 분리나 외부 IAM 연동으로 이동할 수 있다.

## Decision

Tenant source of truth는 `pabal-tenant-*` 모듈과 `pabal_tenant` table이 소유한다. User 생성과 Workspace 생성은 common `TenantContract`를 통해 active tenant 여부를 확인한다.

Workspace source of truth는 `pabal-workspace-*` 모듈과 `workspace`, `workspace_member` table이 소유한다. Workspace 생성은 `TenantContract`와 `UserContract`로 active tenant와 active owner user를 확인하고, owner를 `workspace_member`에 `OWNER`, `ACTIVE`로 저장한다.

Messenger는 user repository, workspace repository, JPA entity를 직접 참조하지 않는다. Messenger infrastructure의 `ContractRoomParticipantDirectoryAdapter`가 common `UserContract`와 `WorkspaceContract`를 사용해 active tenant user와 active workspace member를 조회한다.

DB FK는 bounded context 내부 정합성에 집중한다. `workspace_member`는 `(tenant_id, workspace_id)` 복합 FK로 `workspace`와 연결하지만, `tenant_user` 또는 messenger table과는 직접 FK로 결합하지 않는다. Cross-context 정합성은 application contract와 테스트로 보장한다.

## Consequences

### Positive

- tenant, workspace, user, messenger의 데이터 소유권이 명확해진다.
- Messenger application은 user/workspace persistence 구현을 몰라도 participant validation을 수행할 수 있다.
- workspace participant 검증은 current principal fallback이 아니라 실제 workspace membership을 기준으로 수행된다.
- 향후 MSA 전환 시 `TenantContract`, `UserContract`, `WorkspaceContract`가 동기 API, event projection, cache 경계 후보가 된다.

### Negative

- 단일 DB 안에서도 cross-context FK를 일부 의도적으로 두지 않으므로 application contract 테스트가 중요해진다.
- tenant/workspace/user 생성 순서가 테스트 fixture와 local seed data에 반영되어야 한다.

### Follow-up

- module dependency rule을 자동 검증한다.
- contract 기반 조회를 MSA 전환 시 어떤 외부 계약으로 바꿀지 별도 설계한다.

## Related

- [Pabal 패키지 구조와 레이어](../architecture/package-structure-and-layers.md)
- [Pabal 멀티모듈 전환 전략](../architecture/multi-module-transition.md)
- [Pabal 인가 경계와 멀티테넌시 체크포인트](../security/authorization-and-multitenancy.md)
- [Pabal 데이터베이스 스키마와 제약](../architecture/database-schema-and-constraints.md)
