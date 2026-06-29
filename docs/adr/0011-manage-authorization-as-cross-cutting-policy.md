# ADR-0011: Manage authorization as cross-cutting RBAC permission policy

> 상위 문서: [ADR 목록](README.md)  
> 관련 문서: [Pabal Authorization Governance와 RBAC Permission 모델](../security/authorization-governance.md), [Pabal 인가 경계와 멀티테넌시 체크포인트](../security/authorization-and-multitenancy.md), [Pabal 보안과 JWT Claim 설계](../security/jwt-claim-design.md)

## Status

Accepted

## Context

Pabal은 `pabal-tenant-*`, `pabal-workspace-*`, `pabal-user-*`, `pabal-messenger-*`로 나뉜 멀티모듈 모놀리스다. 인증은 JWT 기반이며, API와 STOMP 모두 `PabalPrincipal(userId, tenantId, subject)`를 기준으로 tenant/user context를 전달한다.

기존 Messenger channel 권한은 fine-grained `MessengerPermission`과 `PermissionPort`로 분리되어 있었지만, tenant owner/admin, workspace owner/admin, JWT authority, `workspace_member.role`의 책임 경계가 문서상 충분히 고정되어 있지 않았다. 엔터프라이즈 애플리케이션에서는 인가가 여러 모듈에 걸친 cross-cutting 관심사이므로, security module, application policy, infrastructure adapter, bounded context source of truth의 책임을 명확히 나눠야 한다.

## Decision

Pabal은 authorization을 cross-cutting RBAC permission policy로 관리한다.

- `pabal-security`는 JWT 검증, principal 생성, authenticated context, access/refresh token lifecycle, HTTP security를 담당한다.
- `pabal-authorization`은 authority normalization, authority/permission matching, cross-cutting RBAC permission lookup/cache를 담당한다.
- `pabal-infra-redis`는 Redis starter dependency를 모아 authorization cache와 향후 module cache layer가 재사용할 Redis infrastructure boundary를 제공한다.
- JWT converter는 role-permission matrix를 알지 않는다.
- `AuthorityNormalizer`와 `PermissionAuthorityMatcher`로 JWT role/scope/permission authority 비교 규칙을 공통화한다.
- `pabal-authorization`은 `RbacPermissionStore`를 통해 tenant/user의 resolved permission value를 조회한다.
- `pabal-security`는 opaque refresh token을 DB hash로 저장하고 refresh 시 rotate한다. 이 JDBC dependency는 refresh token 보안 인프라에 한정하고, RBAC authorization JDBC/Redis 구현은 `pabal-authorization`에 둔다.
- `pabal-app` Flyway migration은 `rbac_permission`, `rbac_role`, `rbac_role_permission`, `rbac_user_role`을 cross-cutting authorization policy store로 관리한다.
- `pabal-app` Flyway migration은 `security_refresh_token`을 refresh token revocation/rotation store로 관리한다.
- `rbac_permission` catalog는 bounded context application module의 `FineGrainedPermission` enum과 동기화한다.
- Use case permission requirement는 application layer가 결정한다.
- Domain model은 security principal, JWT, role, permission adapter를 알지 않는다.
- `pabal-common`은 `FineGrainedPermission` interface와 bounded context 간 최소 contract만 제공한다.
- Fine-grained permission enum은 각 bounded context application module이 소유한다.
- Infrastructure authorization adapter는 JWT authority, persisted RBAC permission, module-owned role source를 application `PermissionPort` 결과로 변환한다.
- Workspace owner/admin 판정은 `WorkspaceContract.findActiveMemberRole`을 통해 `workspace_member.role` source of truth를 조회한다.
- Module 간 권한 판단은 JPA/repository 공유가 아니라 common contract와 application policy로 통제한다.

## Consequences

### Positive

- JWT claim normalization과 business authorization policy가 분리된다.
- role은 coarse-grained input으로 유지하고, use case는 fine-grained permission으로 검증된다.
- persisted role-permission mapping을 사용해 tenant/user별 권한 변경을 access token claim 변경보다 빠르게 반영할 수 있다.
- 짧은 access token과 refresh token revoke를 조합해 권한 변경 후 장기 세션을 차단할 수 있다.
- workspace role source of truth를 workspace module에 유지하면서 Messenger 권한에 연결할 수 있다.
- 각 bounded context가 자기 permission catalog를 소유할 수 있어 future MSA extraction에 유리하다.
- domain purity와 module dependency rules를 보존한다.

### Negative

- 권한 판단이 `CurrentAuthenticationProvider`, `PermissionPort`, bounded context contract를 거치므로 단순 role check보다 코드 경로가 길다.
- permission enum, Flyway seed, role-permission matrix, IAM claim 발급 정책을 문서/테스트로 계속 동기화해야 한다.
- Redis read-through cache와 refresh token revoke 기능은 구현되어 있지만 role/assignment 관리 API가 아직 없어 호출자는 후속 작업으로 남아 있다.
- `pabal-authorization`이 JDBC 기반 RBAC 조회 구현을 포함하므로, 향후 authorization 관리 API가 커지면 command/query API와 persistence implementation을 추가로 나눌 수 있다.
- `pabal-security`는 refresh token rotation 저장소 때문에 `spring-jdbc`를 유지한다. 이는 RBAC authorization infrastructure 분리 범위 밖의 security infrastructure 결정이다.

### Follow-up

- tenant owner/admin bootstrap과 role administration API를 구현한다.
- workspace/user management permission enforcement를 use case에 연결한다.
- role/assignment 관리 API에서 Redis permission cache evict와 refresh token revoke 호출을 연결한다.
- authorization decision audit log를 설계한다.
- Gradle/module dependency rule 자동 검증을 추가한다.
- 운영 IAM에서 scoped role/permission claim 발급 규칙을 확정한다.
