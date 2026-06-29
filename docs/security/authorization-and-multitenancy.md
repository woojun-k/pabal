---
tags:
  - pabal
  - security
  - authorization
  - multitenancy
---

# Pabal 인가 경계와 멀티테넌시 체크포인트

> 상위 문서: [Pabal 상세 설계 허브](../design/design-hub.md)
> 관련 문서: [Pabal Authorization Governance와 RBAC Permission 모델](authorization-governance.md), [Pabal 보안과 JWT Claim 설계](jwt-claim-design.md), [Pabal STOMP 연동 가이드](../realtime/stomp-guide.md), [Pabal 런타임 흐름](../architecture/runtime-flow.md), [Pabal Command-Query 유스케이스 카탈로그](../use-cases/command-query-catalog.md)

## 기본 원칙

Layer: Security → API → Application → Application Port → Infrastructure

- tenant/user context의 source of truth는 `PabalPrincipal`이다.
- tenant 존재/상태 source of truth는 `pabal_tenant`이며, bounded context 간 조회는 `TenantContract`로 수행한다.
- tenant 안의 user 존재/상태 source of truth는 `tenant_user`이며, bounded context 간 조회는 `UserContract`로 수행한다.
- workspace 존재와 workspace membership source of truth는 `workspace`, `workspace_member`이며, Messenger는 `WorkspaceContract`로 active workspace member를 조회한다.
- tenant-scoped RBAC role/permission assignment source of truth는 `rbac_role`, `rbac_user_role`, `rbac_role_permission`, `rbac_permission`이다.
- repository 조회 조건에는 `tenantId`가 포함되어야 한다.
- room 접근은 room 상태와 active membership을 모두 확인해야 한다.
- client-provided identity는 거부하거나 principal과 비교해야 한다.

## HTTP 경계

Layer: API

- `/api/v1/tenant-registrations/**`는 tenant 생성 전 onboarding 경로이므로 public이다. 이 경로는 principal tenant/user를 신뢰하지 않고 DNS TXT 소유권 검증으로 tenant 활성화를 통제한다.
- `ChatCommandMapper`는 `Authentication`에서 `PabalPrincipal`을 추출한다.
- `ChatQueryMapper`도 동일하게 principal에서 `tenantId`, `userId`를 추출한다.
- `UserCommandMapper`는 `/users/me` 생성 시 principal의 `tenantId`, `userId`와 request `name`으로 command를 만든다.
- `UserQueryMapper`는 `/users/me`에서는 principal user를, `/users/{userId}`에서는 path user와 principal tenant를 query에 넣는다.
- `WorkspaceCommandMapper`는 `/workspaces` 생성 시 principal의 `tenantId`, `userId`와 request `name`으로 command를 만든다.
- `WorkspaceQueryMapper`는 `/workspaces/{workspaceId}` 조회 시 principal tenant와 path workspaceId로 query를 만든다.
- request body에는 userId/tenantId를 받지 않는다.
- principal이 없으면 `AccessDeniedException`이 발생한다.

## Application authorization checkpoints

Layer: Application

| 목적 | 코드 | 검증 |
| --- | --- | --- |
| send/typing | `ChatRoomAccessSupport.loadSendableActiveMember` | room send 가능, active member |
| read/query | `ChatRoomReadAccessSupport.loadReadableActiveMember` | room read 가능, active member |
| join | `ChatRoomAccessSupport.loadJoinableRoom`, `RoomMembershipPolicy` | `ACTIVE` public channel self-join만 허용 |
| leave | `ChatRoomAccessSupport.loadLeavableMember` | active member |
| edit/delete message | `EditMessageCommandHandler`, `DeleteMessageCommandHandler` | `chatRoomId` 포함 message 조회, active member와 sendable room 재검증, requester sender 확인 |
| channel create | `ChatRoomAuthorizationService` | `messenger:channel:create` permission |
| room/channel invite | `RoomParticipantPolicy`, `ChatRoomAuthorizationService` | requester+participants batch membership 검증, group은 `messenger:room:invite`, channel은 `messenger:channel:invite` permission |
| room deletion | `ChatRoomDeletionSupport`, `ChatRoomAuthorizationService` | own/any deletion permission |
| tenant registration | `RequestTenantRegistrationCommandHandler`, `PollTenantDomainVerificationsCommandHandler`, `VerifyTenantDomainCommandHandler` | public onboarding 요청, scheduler DNS TXT polling 후 tenant 활성화 |
| tenant dev create/read | `CreateTenantCommandHandler`, `GetTenantQueryHandler` | local/test profile의 `/dev/tenants/**` seed/debug 경로 |
| user create/read | `CreateUserCommandHandler`, `GetUserQueryHandler` | active tenant 안에서 principal user 생성, principal tenant 범위의 user 조회 |
| workspace create/read | `CreateWorkspaceCommandHandler`, `GetWorkspaceQueryHandler` | active tenant와 active owner user 검증, principal tenant 범위의 workspace 조회 |
| user existence | `UserContractService`, `RoomParticipantPolicy` | tenant active user 여부를 user module contract로 검증 |
| workspace membership | `WorkspaceContractService`, `RoomParticipantPolicy` | channel participant가 active workspace member인지 workspace module contract로 검증 |

## RBAC와 fine-grained permission

Layer: Application Port / Infrastructure

Application은 role 이름을 직접 해석하지 않고 `PermissionPort`에 `PermissionCheck`를 전달한다. `PermissionCheck.permission`은 공통 `FineGrainedPermission` abstraction을 사용하며, 현재 Messenger bounded context는 다음 action contract를 구현한다.

권한 거버넌스 전체 규칙과 module별 role/permission ownership은 [Pabal Authorization Governance와 RBAC Permission 모델](authorization-governance.md)을 기준으로 한다.

| Permission | 값 | 용도 |
| --- | --- | --- |
| `CHANNEL_CREATE` | `messenger:channel:create` | workspace 안에서 channel 생성 |
| `CHANNEL_INVITE` | `messenger:channel:invite` | workspace channel 생성/초대 대상 추가 |
| `ROOM_INVITE` | `messenger:room:invite` | tenant group room 초대 대상 추가 |
| `CHANNEL_DELETE_SCHEDULE_OWN` | `messenger:channel:delete:schedule:own` | 자신이 생성한 channel 삭제 예약 |
| `CHANNEL_DELETE_SCHEDULE_ANY` | `messenger:channel:delete:schedule:any` | channel 삭제 예약 관리자 권한 |
| `CHANNEL_DELETE_EXECUTE_OWN` | `messenger:channel:delete:execute:own` | 자신이 생성한 channel 즉시 삭제 |
| `CHANNEL_DELETE_EXECUTE_ANY` | `messenger:channel:delete:execute:any` | channel 즉시 삭제 관리자 권한 |

`RbacPermissionAdapter`는 현재 인증과 DB RBAC store를 다음 기준으로 permission에 매핑한다.

| Grant source | Permission |
| --- | --- |
| active `rbac_user_role` + active `rbac_role` + `rbac_role_permission` | 연결된 `rbac_permission.value` |
| `ROLE_PABAL_ADMIN`, `ROLE_TENANT_OWNER`, `ROLE_TENANT_ADMIN`, `ROLE_TENANT_{NORMALIZED_TENANT_ID}_OWNER`, `ROLE_TENANT_{NORMALIZED_TENANT_ID}_ADMIN` | 모든 Messenger permission |
| `ROLE_WORKSPACE_OWNER`, `ROLE_WORKSPACE_ADMIN`, `ROLE_WORKSPACE_{NORMALIZED_WORKSPACE_ID}_OWNER`, `ROLE_WORKSPACE_{NORMALIZED_WORKSPACE_ID}_ADMIN` | channel create, channel invite, schedule any, execute any |
| active `workspace_member.role` `OWNER`, `ADMIN` | 해당 workspace의 channel create, channel invite, schedule any, execute any |
| `ROLE_CHANNEL_OWNER` | schedule own, execute own |
| `SCOPE_{permission}`, raw `{permission}`, `PERMISSION_{NORMALIZED_PERMISSION}` | 해당 permission 직접 부여 |
| `tenant:{tenantId}:{permission}`, `user:{userId}:{permission}`, `workspace:{workspaceId}:{permission}`, `room:{chatRoomId}:{permission}`와 `SCOPE_` variant | scope가 일치하는 해당 permission |

`ANY`는 "아무나"가 아니라 target room의 creator 여부와 무관하게 수행할 수 있는 관리자 permission이다. creator가 요청하면 `OWN`, creator가 아니면 `ANY` permission을 요구한다.

권한 변경 즉시성:

- DB RBAC role/assignment 변경은 다음 permission check부터 반영된다.
- JWT role/permission claim 변경은 access token 만료나 재발급 이후 반영된다.
- 운영에서는 short-lived access token, opaque refresh token rotation, DB/Redis-backed RBAC lookup을 기본으로 둔다.
- role/assignment 변경 use case는 refresh token revoke와 RBAC cache evict를 함께 수행해야 한다.

## SecurityContext boundary

Layer: Security / Infrastructure

`SecurityContextHolder` 직접 접근은 `pabal-security`의 `CurrentAuthenticationProvider`가 캡슐화한다. Messenger infrastructure의 `RbacPermissionAdapter`는 이 provider만 의존하고, application은 `PermissionPort`만 의존한다.

`pabal-security`는 Spring Messaging 타입을 public signature로 노출하지 않는다. STOMP 전용 `DestinationUserNameProvider` 구현은 messenger infrastructure의 `StompAuthenticationToken`과 `RealtimePrincipal`에 둔다.

## Repository tenant checks

Layer: Application Port / Infrastructure

대표 메서드:

- `findByTenantIdAndId`
- `findByTenantIdAndChatRoomIdAndId`
- `findByTenantIdAndChatRoomIdAndUserId`
- `findAllActiveByTenantIdAndUserId`
- `findByTenantIdAndUserIds`
- `existsByTenantIdAndIdAndStatus`
- `findActiveUserIdsInTenant`
- `findActiveMemberIds`

체크포인트:

- 신규 read/write repository method에 `tenantId` 조건이 있는가?
- native query에 `tenant_id = :tenantId`가 들어가는가?
- unique 제약이 tenant 범위를 포함하는가?

## STOMP authorization checkpoints

Layer: Infrastructure

Code: `RoomSubscriptionAuthorizationManager`

검증 순서:

1. authentication 존재 및 authenticated 여부
2. authentication principal이 `PabalPrincipal`인지 확인
3. destination pattern 파싱
4. destination tenant와 principal tenant 비교
5. room 존재와 `canSubscribe()` 확인
6. active membership 확인

CONNECT 단계에서는 `StompConnectAuthenticationInterceptor`가 JWT 인증 결과를 `StompAuthenticationToken`으로 감싸 `accessor.setUser(...)`에 넣는다. 이 wrapper는 principal은 `PabalPrincipal`로 유지하고, `/user` destination routing에 필요한 user name만 `RealtimePrincipal.destinationUserName(tenantId, userId)` 규칙으로 제공한다.

`ChatRealtimeCommandController`는 typing/send payload tenant와 principal tenant도 비교한다.

## 현재 구현 / 남은 영역

Status: Partially Implemented

- channel create/deletion은 RBAC adapter와 fine-grained permission으로 보호한다.
- tenant registration은 DNS TXT verification 기반 MVP로 구현되어 있다. `PENDING_VERIFICATION` row를 queue item처럼 사용해 기본 600초 간격으로 자동 polling하고, 즉시 recheck는 local/test dev endpoint로만 유지한다. tenant owner/admin은 RBAC table로 표현할 수 있지만, bootstrap, domain 추가/이전, admin API는 후속 설계가 필요하다.
- workspace membership의 source of truth는 `workspace_member`와 `WorkspaceContract`로 구현되어 있다.
- workspace owner/admin permission은 `workspace_member.role`과 JWT role authority 양쪽에서 fine-grained Messenger permission으로 변환한다.
- tenant/user/workspace permission catalog는 각각 `TenantPermission`, `UserPermission`, `WorkspacePermission`으로 구현되어 있고, management endpoint enforcement는 후속 작업이다.
- private channel direct self-join은 이미 거부한다. 초대/admin approval 기반 멤버 추가 흐름은 별도 membership policy로 확장해야 한다.
- PostgreSQL RLS는 현재 적용하지 않았다. system-level DB credential을 쓰는 애플리케이션에서는 request tenant context를 세션 변수로 주입하는 별도 설계가 필요하다.

## 변경 시 체크리스트

- [ ] 새 endpoint가 principal tenant/user를 사용하는가?
- [ ] request body의 tenant/user 값을 신뢰하지 않는가?
- [ ] 새 repository method가 tenant 조건을 포함하는가?
- [ ] realtime destination에 tenant/room authorization이 붙어 있는가?
- [ ] domain invariant와 authorization policy가 섞이지 않았는가?
- [ ] 테스트에 다른 tenant 접근 실패 케이스가 있는가?
