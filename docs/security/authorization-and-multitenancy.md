---
tags:
  - pabal
  - security
  - authorization
  - multitenancy
---

# Pabal 인가 경계와 멀티테넌시 체크포인트

> 상위 문서: [Pabal 상세 설계 허브](../design/design-hub.md)
> 관련 문서: [Pabal 보안과 JWT Claim 설계](jwt-claim-design.md), [Pabal STOMP 연동 가이드](../realtime/stomp-guide.md), [Pabal 런타임 흐름](../architecture/runtime-flow.md), [Pabal Command-Query 유스케이스 카탈로그](../use-cases/command-query-catalog.md)

## 기본 원칙

Layer: Security → API → Application → Application Port → Infrastructure

- tenant/user context의 source of truth는 `PabalPrincipal`이다.
- repository 조회 조건에는 `tenantId`가 포함되어야 한다.
- room 접근은 room 상태와 active membership을 모두 확인해야 한다.
- client-provided identity는 거부하거나 principal과 비교해야 한다.

## HTTP 경계

Layer: API

- `ChatCommandMapper`는 `Authentication`에서 `PabalPrincipal`을 추출한다.
- `ChatQueryMapper`도 동일하게 principal에서 `tenantId`, `userId`를 추출한다.
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
| room deletion | `ChatRoomDeletionSupport`, `ChatRoomAuthorizationService` | own/any deletion permission |

## RBAC와 fine-grained permission

Layer: Application Port / Infrastructure

Application은 role 이름을 직접 해석하지 않고 `PermissionPort`에 `PermissionCheck`를 전달한다. permission 값은 Messenger bounded context의 action contract이며, 현재 값은 다음과 같다.

| Permission | 값 | 용도 |
| --- | --- | --- |
| `CHANNEL_CREATE` | `messenger:channel:create` | workspace 안에서 channel 생성 |
| `CHANNEL_DELETE_SCHEDULE_OWN` | `messenger:channel:delete:schedule:own` | 자신이 생성한 channel 삭제 예약 |
| `CHANNEL_DELETE_SCHEDULE_ANY` | `messenger:channel:delete:schedule:any` | channel 삭제 예약 관리자 권한 |
| `CHANNEL_DELETE_EXECUTE_OWN` | `messenger:channel:delete:execute:own` | 자신이 생성한 channel 즉시 삭제 |
| `CHANNEL_DELETE_EXECUTE_ANY` | `messenger:channel:delete:execute:any` | channel 즉시 삭제 관리자 권한 |

`RbacPermissionAdapter`는 현재 인증의 authority를 다음 기준으로 permission에 매핑한다.

| Authority | Permission |
| --- | --- |
| `ROLE_PABAL_ADMIN`, `ROLE_TENANT_ADMIN` | 모든 Messenger permission |
| `ROLE_WORKSPACE_ADMIN` | channel create, schedule any, execute any |
| `ROLE_CHANNEL_OWNER` | schedule own, execute own |
| `SCOPE_{permission}`, raw `{permission}`, `PERMISSION_{NORMALIZED_PERMISSION}` | 해당 permission 직접 부여 |
| `tenant:{tenantId}:{permission}`, `workspace:{workspaceId}:{permission}`, `room:{chatRoomId}:{permission}`와 `SCOPE_` variant | scope가 일치하는 해당 permission |

`ANY`는 "아무나"가 아니라 target room의 creator 여부와 무관하게 수행할 수 있는 관리자 permission이다. creator가 요청하면 `OWN`, creator가 아니면 `ANY` permission을 요구한다.

## SecurityContext boundary

Layer: Security / Infrastructure

`SecurityContextHolder` 직접 접근은 `pabal-security`의 `CurrentAuthenticationProvider`가 캡슐화한다. Messenger infrastructure의 `RbacPermissionAdapter`는 이 provider만 의존하고, application은 `PermissionPort`만 의존한다.

## Repository tenant checks

Layer: Application Port / Infrastructure

대표 메서드:

- `findByTenantIdAndId`
- `findByTenantIdAndChatRoomIdAndId`
- `findByTenantIdAndChatRoomIdAndUserId`
- `findAllActiveByTenantIdAndUserId`
- `findByTenantIdAndUserIds`

체크포인트:

- 신규 read/write repository method에 `tenantId` 조건이 있는가?
- native query에 `tenant_id = :tenantId`가 들어가는가?
- unique 제약이 tenant 범위를 포함하는가?

## STOMP authorization checkpoints

Layer: Infrastructure

Code: `RoomSubscriptionAuthorizationManager`

검증 순서:

1. authentication 존재 및 authenticated 여부
2. principal이 `PabalPrincipal`인지 확인
3. destination pattern 파싱
4. destination tenant와 principal tenant 비교
5. room 존재와 `canSubscribe()` 확인
6. active membership 확인

`ChatRealtimeCommandController`는 typing payload tenant와 principal tenant도 비교한다.

## 현재 구현 / 남은 영역

Status: Partially Implemented

- channel create/deletion은 RBAC adapter와 fine-grained permission으로 보호한다.
- workspace/channel role의 source of truth는 아직 외부 Workspace/Identity 모델이 아니라 JWT authority에 있다.
- private channel direct self-join은 이미 거부한다. 초대/admin approval 기반 멤버 추가 흐름은 별도 membership policy로 확장해야 한다.
- PostgreSQL RLS는 현재 적용하지 않았다. system-level DB credential을 쓰는 애플리케이션에서는 request tenant context를 세션 변수로 주입하는 별도 설계가 필요하다.

## 변경 시 체크리스트

- [ ] 새 endpoint가 principal tenant/user를 사용하는가?
- [ ] request body의 tenant/user 값을 신뢰하지 않는가?
- [ ] 새 repository method가 tenant 조건을 포함하는가?
- [ ] realtime destination에 tenant/room authorization이 붙어 있는가?
- [ ] domain invariant와 authorization policy가 섞이지 않았는가?
- [ ] 테스트에 다른 tenant 접근 실패 케이스가 있는가?
