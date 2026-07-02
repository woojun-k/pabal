---
tags:
  - pabal
  - testing
  - test-catalog
---

# Pabal 테스트 케이스 카탈로그

> 상위 문서: [Pabal 상세 설계 허브](../design/design-hub.md)
> 관련 문서: [Pabal 테스트 전략](testing-strategy.md), [Pabal 에러 코드와 예외 매핑표](../use-cases/error-code-exception-mapping.md), [Pabal 엔드포인트 시퀀스 다이어그램](../use-cases/endpoint-sequence-diagrams.md), [Pabal Authorization Governance와 RBAC Permission 모델](../security/authorization-governance.md), [Pabal 인가 경계와 멀티테넌시 체크포인트](../security/authorization-and-multitenancy.md)

## SendMessage 중복 전송

Layer: Application / Infrastructure
Target: `SendMessageCommandHandler`, `MessageSendSupportAdapter`
Purpose: `clientMessageId` 기반 idempotency 보장
Given: 동일 tenant/room/sender/clientMessageId의 기존 메시지
When: 같은 command를 다시 처리
Then: 기존 messageId와 `duplicated=true`를 반환
Related: [Pabal Command-Query 유스케이스 카탈로그](../use-cases/command-query-catalog.md), [Pabal Persistence 경계와 데이터 변환](../architecture/persistence-boundary-and-mapping.md)

## SendReply 대상 room 불일치

Layer: Application / Domain
Target: `SendReplyCommandHandler`, `MessageSendSupport.validateReplyTarget`
Purpose: 다른 room 메시지에 reply를 달 수 없도록 보장
Given: reply target의 `chatRoomId`가 command room과 다름
When: reply command 처리
Then: `InvalidReplyTargetException`, `MSG400001`
Related: [Pabal 에러 코드와 예외 매핑표](../use-cases/error-code-exception-mapping.md)

## MarkRead stale cursor

Layer: Domain / Application
Target: `ChatRoomMember.updateLastRead`, `MarkReadCommandHandler`
Purpose: read cursor가 뒤로 후퇴하지 않도록 보장
Given: member의 `lastReadSequence`가 요청 sequence보다 큼
When: mark read 처리
Then: member update와 `MessageReadEvent` 발행 없음
Related: [Pabal 엔드포인트 시퀀스 다이어그램](../use-cases/endpoint-sequence-diagrams.md)

## TenantRegistration verify-only handler stops at DOMAIN_VERIFIED

Layer: Application / Infrastructure
Target: `VerifyTenantDomainCommandHandler`, `TenantRegistrationCommandHandlerIntegrationTest.verify_checks_dns_txt_and_marks_domain_verified_without_creating_a_tenant`
Purpose: DNS TXT 검증 성공이 `Tenant` 생성 없이 `DOMAIN_VERIFIED`에서 멈추도록 보장(two-phase verify/activate 분리)
Given: `PENDING_VERIFICATION` registration과 일치하는 DNS TXT record
When: `VerifyTenantDomainCommandHandler.handle`
Then: registration이 `DOMAIN_VERIFIED`로 전이하고 `activationExpiresAt`이 설정되지만 `Tenant` row는 생성되지 않음
Related: [ADR-0013](../adr/0013-split-overloaded-expiry-timestamp-into-verification-and-activation-windows.md), [Pabal Command-Query 유스케이스 카탈로그](../use-cases/command-query-catalog.md)

## TenantRegistration activation window/status guard

Layer: Application
Target: `ActivateTenantRegistrationCommandHandler`, `ActivateTenantRegistrationCommandHandlerTest`
Purpose: `DOMAIN_VERIFIED` + window-open 상태에서만 activation이 `Tenant`를 생성하도록 보장
Given: `PENDING_VERIFICATION`/`REVERIFICATION_REQUIRED`/`ACTIVATED`/`EXPIRED` 상태이거나 `activationExpiresAt`이 지난 `DOMAIN_VERIFIED` registration
When: `ActivateTenantRegistrationCommandHandler.handle`
Then: `TenantRegistrationNotPendingException`(`TNT409003`) 또는 `TenantRegistrationExpiredException`(`TNT410001`)을 던지고 `Tenant`는 생성되지 않음
Related: [Pabal HTTP API 예시와 오류 매핑](../use-cases/http-api-and-error-mapping.md)

## TenantRegistration reverification DNS-lookup-before-status-guard

Layer: Application
Target: `ReverifyTenantRegistrationCommandHandler`, `ReverifyTenantRegistrationCommandHandlerTest`
Purpose: 상태가 `REVERIFICATION_REQUIRED`가 아니면 DNS 조회를 전혀 수행하지 않도록 보장
Given: `REVERIFICATION_REQUIRED`가 아닌 상태의 registration
When: `ReverifyTenantRegistrationCommandHandler.handle`
Then: `TenantRegistrationNotPendingException`(`TNT409003`)을 던지고 `DnsTxtLookupPort` 호출 0회
Related: [Pabal Command-Query 유스케이스 카탈로그](../use-cases/command-query-catalog.md)

## TenantRegistration REVERIFICATION_REQUIRED grace-window terminal expiry

Layer: Infrastructure
Target: `TenantRegistrationRepositoryImpl.expireLapsedReverificationRegistrations`, `TenantRegistrationRepositoryImplTest.expireLapsedReverificationRegistrations_includes_row_at_exact_grace_boundary`
Purpose: grace window가 지난 `REVERIFICATION_REQUIRED` row만 `EXPIRED`로 닫히도록 보장
Given: `activationExpiresAt + graceMs <= now`인 row와 grace window 안에 있는 row
When: `expireLapsedReverificationRegistrations(now, graceMs)` bulk UPDATE 실행
Then: grace 경과 row(경계값 포함)만 `EXPIRED`로 전이하고, window 안의 row와 `DOMAIN_VERIFIED` row는 건드리지 않음
Related: [Pabal 데이터베이스 스키마와 제약](../architecture/database-schema-and-constraints.md)

## DirectRoom concurrent create

Layer: Application / Infrastructure
Target: `GetOrCreateDirectRoomCommandHandler`, `DirectRoomCreationService`, `DirectChatMappingRepository`
Purpose: direct room 중복 생성을 방지
Given: 두 요청이 같은 tenant/user pair로 동시에 direct room 생성
When: unique constraint race 발생
Then: `DuplicateDirectChatMappingException` 후 기존 mapping 재조회
Related: [Pabal Persistence 경계와 데이터 변환](../architecture/persistence-boundary-and-mapping.md)

## ChannelName validation

Layer: Domain
Target: `ChannelName`
Purpose: channel name 정규화와 입력 제한 검증
Given: blank, 50자 초과, 허용되지 않는 문자, 대문자 포함 name
When: `new ChannelName(value)`
Then: invalid input 예외 또는 lowercase normalization
Related: [Pabal 도메인 모델 상세](../domain/messenger-domain-model.md)

## Room deletion lifecycle

Layer: Domain / Application
Target: `ChatRoom`, `ChatRoomDeletionSupport`
Purpose: channel deletion 상태 전이를 보장
Given: `GROUP` room 또는 `ACTIVE` channel room
When: deletion schedule/immediate delete 처리
Then: type/status/permission에 맞는 예외 또는 상태 변경
Related: [Pabal 인가 경계와 멀티테넌시 체크포인트](../security/authorization-and-multitenancy.md)

## Public/private channel self join

Layer: Domain / Application
Target: `RoomMembershipPolicy`, `JoinRoomCommandHandler`
Purpose: roomId를 아는 사용자라도 private channel, direct room, group room에 직접 join할 수 없도록 보장
Given: active public channel, active private channel, direct room, group room
When: `canSelfJoin` 또는 join command 처리
Then: public channel만 허용하고 나머지는 `RoomJoinForbiddenException`
Related: [Pabal 도메인 모델 상세](../domain/messenger-domain-model.md), [Pabal 인가 경계와 멀티테넌시 체크포인트](../security/authorization-and-multitenancy.md)

## Channel RBAC permission

Layer: Application / Infrastructure / Security
Target: `ChatRoomAuthorizationService`, `RbacPermissionAdapter`, `PabalJwtAuthenticationConverter`
Purpose: role은 coarse-grained, use case 권한은 fine-grained permission으로 판정
Given: persisted RBAC permission, tenant owner/admin, workspace owner/admin JWT role, workspace_member owner/admin role, channel owner, scoped permission authority
When: channel create, deletion schedule, immediate delete 권한 확인
Then: DB/JWT role/scope에 맞는 permission만 허용하고 다른 tenant/requester principal은 거부
Related: [Pabal Authorization Governance와 RBAC Permission 모델](../security/authorization-governance.md), [Pabal 인가 경계와 멀티테넌시 체크포인트](../security/authorization-and-multitenancy.md), [Pabal 보안과 JWT Claim 설계](../security/jwt-claim-design.md)

## Refresh token lifecycle

Layer: Security
Target: `RefreshTokenService`, `RefreshTokenStore`, `JdbcRefreshTokenStore`
Purpose: 짧은 access token과 opaque refresh token rotation으로 장기 세션을 통제하고 중복 refresh 요청 UX를 보정
Given: 유효한 refresh token, used/revoked refresh token, `X-Request-ID`, user token revoke
When: access token 재발급, duplicate refresh replay, revoke를 수행
Then: 새 access/refresh token pair를 발급하고 3초 grace period 안의 중복 요청은 기존 token pair를 반환하며, grace period 이후 재사용은 token family revoke로 차단한다
Related: [Pabal 보안과 JWT Claim 설계](../security/jwt-claim-design.md), [Pabal Authorization Governance와 RBAC Permission 모델](../security/authorization-governance.md)

## User create duplicate

Layer: Domain / Application / Infrastructure
Target: `CreateUserCommandHandler`, `UserRepositoryImpl`, `tenant_user`
Purpose: principal 기준 tenant/user는 한 번만 등록되도록 보장
Given: 같은 tenant/user의 기존 active user
When: 같은 userId로 `CreateUserCommand` 처리
Then: `DuplicateUserException`, `USR409001`
Related: [Pabal HTTP API 예시와 오류 매핑](../use-cases/http-api-and-error-mapping.md), [Pabal 데이터베이스 스키마와 제약](../architecture/database-schema-and-constraints.md)

## User create active tenant validation

Layer: Application
Target: `CreateUserCommandHandler`, `TenantContract`
Purpose: 존재하지 않거나 inactive tenant에 user가 생성되지 않도록 보장
Given: `TenantContract.existsActiveTenant(tenantId)`가 false
When: `CreateUserCommand` 처리
Then: `InvalidInputException`이 발생하고 clock/save는 호출되지 않음
Related: [Pabal 인가 경계와 멀티테넌시 체크포인트](../security/authorization-and-multitenancy.md)

## Workspace create owner membership

Layer: Application / Infrastructure
Target: `CreateWorkspaceCommandHandler`, `WorkspaceRepositoryImpl`, `WorkspaceMemberRepositoryImpl`
Purpose: active tenant와 active tenant user owner만 workspace를 만들 수 있고 owner membership이 함께 생성되도록 보장
Given: active tenant, active owner user
When: `CreateWorkspaceCommand` 처리
Then: `workspace`가 `ACTIVE`로 저장되고 `workspace_member`에 owner가 `OWNER`, `ACTIVE`로 저장됨
Related: [Pabal HTTP API 예시와 오류 매핑](../use-cases/http-api-and-error-mapping.md), [Pabal 데이터베이스 스키마와 제약](../architecture/database-schema-and-constraints.md)

## Workspace create/isActive domain invariant

Layer: Domain
Target: `Workspace`, `WorkspaceTest`
Purpose: `Workspace.create`가 active workspace를 만들고 `isActive()`가 snapshot status를 기준으로 판정하도록 보장
Given: 새 workspace 생성 입력 또는 `ARCHIVED` status의 `WorkspaceSnapshot`
When: `Workspace.create(...)` 또는 `Workspace.reconstitute(snapshot).isActive()` 호출
Then: 생성된 workspace는 `ACTIVE`이고, reconstituted non-active workspace는 `isActive()` false를 반환함
Related: [Pabal 도메인 모델 상세](../domain/messenger-domain-model.md), [Pabal 테스트 전략](testing-strategy.md)

## WorkspaceMember leave transition

Layer: Domain
Target: `WorkspaceMember`, `WorkspaceMemberSnapshot`, `WorkspaceMemberLeaveNotAllowedException`
Purpose: workspace member `ACTIVE` -> `LEFT` 전이와 마지막 active `OWNER` 보호 invariant 보장
Given: active `MEMBER`/`ADMIN`/`OWNER`, already-left member, null `leftAt`, 다른 active owner evidence
When: `WorkspaceMember.leave(leftAt, hasAnotherActiveOwner)` 호출
Then: 성공 시 같은 instance가 `LEFT`가 되고 `leftAt`/`updatedAt`은 caller-supplied `leftAt`이 되며 identity/role/join/create 값은 유지됨. 이미 `LEFT`인 member와 마지막 active `OWNER` leave는 `WSP409001`로 거부되고, null `leftAt`도 상태 변경 전에 거부됨.
Related: [Pabal 도메인 모델 상세](../domain/messenger-domain-model.md), [Pabal 에러 코드와 예외 매핑표](../use-cases/error-code-exception-mapping.md), [Pabal 테스트 전략](testing-strategy.md)

## WorkspaceMember role change transition

Layer: Domain
Target: `WorkspaceMember`, `WorkspaceMemberChangeRoleTest`, `WorkspaceMemberRoleChangeNotAllowedException`
Purpose: workspace member role 변경과 마지막 active `OWNER` demotion 보호 invariant 보장
Given: active `MEMBER`/`ADMIN`/`OWNER`, already-left member, null `newRole`, null `changedAt`, 다른 active owner evidence
When: `WorkspaceMember.changeRole(newRole, changedAt, hasAnotherActiveOwner)` 호출
Then: 실제 변경은 같은 instance의 `role`과 `updatedAt`만 갱신하고, same-role 호출은 no-op으로 `updatedAt`을 유지함. 이미 `LEFT`인 member와 마지막 active `OWNER` demotion은 `WSP409002`로 거부되며, 거부된 호출과 null 입력은 상태를 부분 변경하지 않음.
Related: [Pabal 도메인 모델 상세](../domain/messenger-domain-model.md), [Pabal 에러 코드와 예외 매핑표](../use-cases/error-code-exception-mapping.md), [Pabal 테스트 전략](testing-strategy.md)

## WorkspaceMemberSnapshot status invariant

Layer: Domain
Target: `WorkspaceMemberSnapshot`, `WorkspaceMemberSnapshotTest`
Purpose: snapshot hydration boundary에서 `ACTIVE`/`LEFT`와 `leftAt` 정합성 보장
Given: `ACTIVE` snapshot with `leftAt` 또는 `LEFT` snapshot without `leftAt`
When: `WorkspaceMemberSnapshot` 생성
Then: 잘못된 status/leftAt 조합은 거부되고, persistence hydration이 domain invariant를 우회하지 못함
Related: [Pabal 도메인 모델 상세](../domain/messenger-domain-model.md), [Pabal Persistence 경계와 데이터 변환](../architecture/persistence-boundary-and-mapping.md)

## Workspace domain time boundary

Layer: Domain
Target: `WorkspaceDomainArchitectureTest`
Purpose: workspace domain model이 domain time을 직접 계산하지 않고 caller-supplied time을 사용하도록 보장
Given: `pabal-workspace-domain/src/main/java` source tree
When: workspace domain Java source에서 `Instant.now(` 사용 여부를 검사
Then: domain source는 `Instant.now()`를 직접 호출하지 않음
Related: [Pabal 도메인 모델 상세](../domain/messenger-domain-model.md), [Pabal 테스트 전략](testing-strategy.md)

## User contract participant validation

Layer: App / Application / Infrastructure
Target: `UserContractService`, `ContractRoomParticipantDirectoryAdapter`, `RoomParticipantPolicy`
Purpose: messenger가 user module의 active tenant user만 초대/참여자로 인정하도록 보장
Given: requester와 participant가 `tenant_user`에 active 상태로 존재
When: messenger participant directory가 target user set을 조회
Then: `UserContract`를 통해 active tenant user 여부가 확인되고, 없는 user는 invitable 대상에서 제외
Related: [Pabal 인가 경계와 멀티테넌시 체크포인트](../security/authorization-and-multitenancy.md)

## Workspace contract participant validation

Layer: App / Application / Infrastructure
Target: `WorkspaceContractService`, `ContractRoomParticipantDirectoryAdapter`, `RoomParticipantPolicy`
Purpose: messenger channel participant validation이 workspace module의 active workspace member만 인정하도록 보장
Given: owner는 `workspace_member`에 active 상태로 존재하고 같은 tenant의 다른 user는 workspace member가 아님
When: `RoomParticipantDirectoryPort.findWorkspaceMemberIds`가 target user set을 조회
Then: `WorkspaceContract`를 통해 active workspace member만 반환되고, tenant user지만 workspace member가 아닌 user는 제외됨
Related: [Pabal 인가 경계와 멀티테넌시 체크포인트](../security/authorization-and-multitenancy.md)

## Message edit/delete access recheck

Layer: Application
Target: `EditMessageCommandHandler`, `DeleteMessageCommandHandler`
Purpose: 탈퇴 사용자나 비활성 room에서 과거 messageId만으로 수정/삭제하지 못하게 보장
Given: requester가 sender지만 active member가 아니거나 room이 sendable 상태가 아님
When: edit/delete command 처리
Then: `ChatRoomAccessSupport.loadSendableActiveMember` 단계에서 거부되고 message update가 발생하지 않음
Related: [Pabal Command-Query 유스케이스 카탈로그](../use-cases/command-query-catalog.md)

## API v1 message sequence contract

Layer: API
Target: `ChatCommandController`, `ChatQueryController`, `MessageResponse`, `SendMessageResponse`
Purpose: HTTP command/query 응답에서 room-local sequence를 클라이언트 reconciliation에 제공
Given: message command/query result에 sequence가 있음
When: send/reply/edit/delete/read/list message endpoint 호출
Then: response body에 `sequence`가 포함되고 endpoint path는 `/api/v1/chat-rooms/...`를 사용
Related: [Pabal HTTP API 예시와 오류 매핑](../use-cases/http-api-and-error-mapping.md)

## ListMessages cursor pagination

Layer: Application / Infrastructure
Target: `ListMessagesHandler`, `MessageReadRepositoryImpl`
Purpose: sequence cursor와 oldest-first response 보장
Given: room에 여러 sequence 메시지
When: cursor와 size로 조회
Then: 내부 조회는 desc, response는 oldest-first, `nextCursor/hasNext` 정확
Related: [Pabal HTTP API 예시와 오류 매핑](../use-cases/http-api-and-error-mapping.md)

## GlobalExceptionHandler validation detail

Layer: Web / API
Target: `GlobalExceptionHandler`
Purpose: validation error를 `ApiError`로 정규화
Given: invalid request body 또는 query parameter
When: controller validation 실패
Then: `CMN002`, details field/reason 포함
Related: [Pabal 에러 코드와 예외 매핑표](../use-cases/error-code-exception-mapping.md)

## STOMP CONNECT authentication

Layer: Security / Infrastructure
Target: `StompConnectAuthenticationInterceptor`, `WebSocketAuthenticationManagerConfig`, `StompAuthenticationToken`
Purpose: STOMP native header token 인증 보장
Given: Authorization bearer, access_token, missing token
When: CONNECT frame 처리
Then: 인증 성공 시 accessor user는 `StompAuthenticationToken`으로 설정되고, 실패 시 denied
Related: [Pabal STOMP 연동 가이드](../realtime/stomp-guide.md), [Websocket 설정](../realtime/websocket-configuration.md)

## STOMP message send MVP

Layer: App / API / Application / Infrastructure
Target: `ChatRealtimeCommandController`, `SendMessageCommandHandler`, `StompChatRealtimeAdapter`
Purpose: WebSocket realtime 송수신 경로를 E2E로 증명
Given: test fixture가 active `pabal_tenant`, `tenant_user`, `chat_room`, `chat_room_member`를 직접 구성하고 receiver가 room event topic을 subscribe
When: sender가 `SEND /app/chat.message.send` payload를 전송
Then: receiver가 room events topic에서 `MESSAGE_SENT` envelope를 수신
Related: [Pabal STOMP 연동 가이드](../realtime/stomp-guide.md), [Pabal Realtime 이벤트 스키마](../realtime/event-schema.md)

## Room subscription authorization

Layer: Infrastructure / Application Port
Target: `RoomSubscriptionAuthorizationManager`
Purpose: tenant isolation과 active membership 기반 subscribe 인가
Given: tenant mismatch, missing room, inactive member, active member
When: room event/typing topic subscribe
Then: 조건에 따라 grant/deny
Related: [Pabal 인가 경계와 멀티테넌시 체크포인트](../security/authorization-and-multitenancy.md)

## Persistence message length policy

Layer: Domain / API / Infrastructure
Target: `SendMessageRequest`, `MessageContent`, `V2__messenger_tables.sql`
Purpose: 메시지 길이 정책 일관성 검증
Given: 256자 이상 5000자 이하 메시지
When: HTTP validation과 domain 생성, DB 저장
Then: 현재 기준으로 정책 불일치가 드러나야 함
Related: [Pabal Persistence 경계와 데이터 변환](../architecture/persistence-boundary-and-mapping.md)
