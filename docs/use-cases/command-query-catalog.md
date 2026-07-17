---
tags:
  - pabal
  - usecase
  - cqrs
---

# Pabal Command-Query 유스케이스 카탈로그

> 상위 문서: [Pabal 상세 설계 허브](../design/design-hub.md)
> 관련 문서: [Pabal 도메인 모델 상세](../domain/messenger-domain-model.md), [Pabal 엔드포인트 시퀀스 다이어그램](endpoint-sequence-diagrams.md), [Pabal HTTP API 예시와 오류 매핑](http-api-and-error-mapping.md), [Pabal 에러 코드와 예외 매핑표](error-code-exception-mapping.md), [Pabal 런타임 흐름](../architecture/runtime-flow.md)

## 개요

Layer: API → Application → Domain → Application Port → Infrastructure Adapter
Status: Implemented

Pabal의 HTTP 계약은 `/api/v1` 아래의 리소스 중심 endpoint로 노출된다. API controller는 외부 request를 application command/query record로 변환하고, application handler가 유스케이스를 처리한다.

## Tenant HTTP 유스케이스

| Use Case | Endpoint | Command/Query | Handler | 주요 domain/port | Event |
| --- | --- | --- | --- | --- | --- |
| RequestTenantRegistration | `POST /api/v1/tenant-registrations` | `RequestTenantRegistrationCommand` | `RequestTenantRegistrationCommandHandler` | `TenantRegistration`, `TenantRegistrationRepository`, `TenantVerificationTokenGeneratorPort` | none |
| RenewTenantRegistrationToken | `POST /api/v1/tenant-registrations/{registrationId}/verification-token` | `RenewTenantRegistrationTokenCommand` | `RenewTenantRegistrationTokenCommandHandler` | `TenantRegistration`, `TenantVerificationTokenGeneratorPort` | none |
| GetTenantRegistration | `GET /api/v1/tenant-registrations/{registrationId}` | `GetTenantRegistrationQuery` | `GetTenantRegistrationQueryHandler` | `TenantRegistrationRepository`, `TenantRegistrationDto` | none |
| VerifyTenantDomain | scheduler internal, dev `POST /dev/tenant-registrations/{registrationId}/domain-verification` | `VerifyTenantDomainCommand` | `VerifyTenantDomainCommandHandler` | `TenantRegistration`, `DnsTxtLookupPort` | none |
| ActivateTenantRegistration | `POST /api/v1/tenant-registrations/{registrationId}/activation` | `ActivateTenantRegistrationCommand` | `ActivateTenantRegistrationCommandHandler` | `TenantRegistration`, `TenantRegistrationRepository`, `Tenant`, `TenantRepository` | none |
| ReverifyTenantRegistration | `POST /api/v1/tenant-registrations/{registrationId}/reverification` | `ReverifyTenantRegistrationCommand` | `ReverifyTenantRegistrationCommandHandler` | `TenantRegistration`, `DnsTxtLookupPort`, `TenantRegistrationRepository` | none |

### TenantRegistration domain 전이 모델

Status: Implemented
Layer: Domain / Contract / Infrastructure / Application / API

`pabal-tenant-domain`의 `TenantRegistration`은 다음 5개 status와 두 개의 독립된 만료 시각을 사용한다. `TenantRegistrationSnapshot`(domain)과 `TenantRegistrationState`(contract, `pabal-tenant-contract`)도 두 timestamp를 함께 보관한다.

- `PENDING_VERIFICATION` → (검증 성공, `verifiedAt < verificationExpiresAt`) → `DOMAIN_VERIFIED`
- `DOMAIN_VERIFIED` → (activation window 경과, `!now.isBefore(activationExpiresAt)`) → `REVERIFICATION_REQUIRED`
- `REVERIFICATION_REQUIRED` → (`reverify`) → `DOMAIN_VERIFIED` (`activationExpiresAt` 갱신)
- `DOMAIN_VERIFIED` → (`activate`, `activatedAt < activationExpiresAt`) → `ACTIVATED`
- `PENDING_VERIFICATION` / `DOMAIN_VERIFIED` / `REVERIFICATION_REQUIRED` → (`expire`) → `EXPIRED`

핵심 규칙:

- `verificationExpiresAt`은 `PENDING_VERIFICATION`의 검증 마감이고, `activationExpiresAt`은 `DOMAIN_VERIFIED`의 활성화 마감이다. 하나의 `expiresAt`이 아니라 두 필드로 분리되어 있다.
- `activate()`는 `activatedAt`이 `activationExpiresAt` 이상이면 `TenantRegistrationExpiredException`을 던진다. `DOMAIN_VERIFIED`가 아니면(`REVERIFICATION_REQUIRED` 포함) `TenantRegistrationNotPendingException`을 던진다.
- `requireReverification(now)`는 activation window가 실제로 열려 있으면(`now.isBefore(activationExpiresAt)`) `IllegalStateException`을 던진다.
- `validateReverificationAllowed()`는 상태가 `REVERIFICATION_REQUIRED`가 아니면 `TenantRegistrationNotPendingException`을 던지는 읽기 전용 guard다. `ReverifyTenantRegistrationCommandHandler`가 DNS 조회 전에 호출해 상태를 먼저 검증한다.
- `REVERIFICATION_REQUIRED`는 `isOpen()`이 true를 반환하는 복구 가능한 상태이며, terminal 상태는 `EXPIRED` 하나뿐이다.
- 이 전이 모델과 결정 배경은 [ADR-0013](../adr/0013-split-overloaded-expiry-timestamp-into-verification-and-activation-windows.md)에 있다.

**모든 계층이 이 모델을 반영한다.**

Persistence: `pabal-tenant-infrastructure`의 `TenantRegistrationEntity`는 `V7__tenant_registration_tables.sql`의 `verificationExpiresAt`/`activationExpiresAt` 두 컬럼과 5-status를 그대로 매핑한다. `TenantRegistrationExpirationScheduler`가 세 개의 독립된 sweep을 실행한다.

| Command | Handler | 설명 | 주기 |
| --- | --- | --- | --- |
| `ExpireTenantRegistrationsCommand` | `ExpireTenantRegistrationsCommandHandler` | `verification_expires_at`이 지난 `PENDING_VERIFICATION` registration을 `EXPIRED`로 닫는다(count 반환). `DOMAIN_VERIFIED`/`REVERIFICATION_REQUIRED`는 건드리지 않는다. | `pabal.tenant.registration.expiration-sweep-delay-ms`(기본 600000ms) |
| `ReverifyLapsedTenantRegistrationsCommand` | `ReverifyLapsedTenantRegistrationsCommandHandler` | `activation_expires_at`이 지난 `DOMAIN_VERIFIED` registration을 도메인 메서드 `requireReverification(now)`를 통해서만 `REVERIFICATION_REQUIRED`로 전이한다(count 반환). row 단위로 실패에 견고하다 — 조회 이후 상태/window가 바뀐 row는 건너뛰고 카운트하지 않는다. | `pabal.tenant.registration.reverification-sweep-delay-ms`(기본 600000ms) |
| (command 없음, repository bulk UPDATE) | `TenantRegistrationRepository.expireLapsedReverificationRegistrations(Instant, long)` | `activationExpiresAt + reverification-grace-ms <= now`인 `REVERIFICATION_REQUIRED` row를 bulk UPDATE로 `EXPIRED`로 닫는다(terminal expiry, `expirePendingRegistrations`와 동일한 bulk-UPDATE 패턴). grace window 안의 row는 건드리지 않는다. `DOMAIN_VERIFIED` row는 이 sweep의 대상이 아니다 — 먼저 위 sweep으로 `REVERIFICATION_REQUIRED`를 거쳐야 한다. | `TenantRegistrationExpirationScheduler.expirePendingRegistrations()`에 포함되어 `pabal.tenant.registration.expiration-sweep-delay-ms`(기본 600000ms)마다 실행. grace 길이는 `pabal.tenant.registration.reverification-grace-ms`(기본 604800000ms = 7일) |

Application/API: 진짜 two-phase 흐름이 도입되었다.

- `VerifyTenantDomainCommandHandler`(verify-only)는 DNS TXT 검증 후 `markVerified(now, now+activation-window)`로 `PENDING_VERIFICATION` → `DOMAIN_VERIFIED`에서 멈춘다. `Tenant`를 생성하지 않으며 `TenantRepository` 의존성 자체가 없다. scheduler의 `PollTenantDomainVerificationsCommandHandler`가 내부적으로 호출하며, 이 handler는 HTTP endpoint로 직접 노출되지 않는다(local/test dev endpoint `POST /dev/tenant-registrations/{registrationId}/domain-verification`만 예외).
- 새 `ActivateTenantRegistrationCommandHandler`(`POST /api/v1/tenant-registrations/{registrationId}/activation`)가 activation을 명시적 단계로 분리한다. `findByIdForUpdate` 락 이후 상태/window를 읽기 전용으로 먼저 확인해 fail-fast하고, `Tenant.create` + `activate(tenantId, now)`로 `Tenant`를 정확히 1건 생성해 registration에 연결한다. 실패 시 `Tenant`는 저장되지 않는다(트랜잭션 롤백).
- 새 `ReverifyTenantRegistrationCommandHandler`(`POST /api/v1/tenant-registrations/{registrationId}/reverification`)가 단일 registration의 명시적 재검증을 담당한다. `validateReverificationAllowed()`로 DNS 조회 전에 상태를 먼저 검증하므로, 상태가 `REVERIFICATION_REQUIRED`가 아니면 DNS 조회 없이 실패한다. 상태가 맞으면 기존 verification token으로 DNS TXT를 재확인하고 `reverify(now, now+activation-window)`로 `DOMAIN_VERIFIED`로 복귀시킨다. 이 handler는 스케줄러 sweep인 `ReverifyLapsedTenantRegistrationsCommandHandler`와 별개다(전자는 단일 registration 명시적 트리거, 후자는 전체 lapsed row 배치 sweep).
- `TenantRegistrationResult`, `TenantRegistrationDto`, API 응답(`TenantRegistrationResponse`, `TenantRegistrationDetailResponse`, `VerifyTenantDomainResponse`, `ActivateTenantRegistrationResponse`, `ReverifyTenantRegistrationResponse`)은 모두 단일 `expiresAt` 대신 `verificationExpiresAt`/`activationExpiresAt`을 노출한다. `GET /api/v1/tenant-registrations/{registrationId}`는 5-status 문자열을 그대로 반환한다.
- 상세 endpoint 계약은 [Pabal HTTP API 예시와 오류 매핑](http-api-and-error-mapping.md#tenant-endpoints)을 참고한다.

## Tenant Dev HTTP 유스케이스

| Use Case | Endpoint | Command/Query | Handler | 주요 domain/port | Event |
| --- | --- | --- | --- | --- | --- |
| DevCreateTenant | `POST /dev/tenants` | `CreateTenantCommand` | `CreateTenantCommandHandler` | `Tenant`, `TenantRepository` | none |
| DevGetTenant | `GET /dev/tenants/{tenantId}` | `GetTenantQuery` | `GetTenantQueryHandler` | `TenantRepository`, `TenantDto` | none |

## User HTTP 유스케이스

| Use Case | Endpoint | Command/Query | Handler | 주요 domain/port | Event |
| --- | --- | --- | --- | --- | --- |
| CreateMe | `POST /api/v1/users/me` | `CreateUserCommand` | `CreateUserCommandHandler` | `TenantContract`, `User`, `UserRepository` | none |
| GetMe | `GET /api/v1/users/me` | `GetUserQuery` | `GetUserQueryHandler` | `UserRepository`, `UserDto` | none |
| GetUser | `GET /api/v1/users/{userId}` | `GetUserQuery` | `GetUserQueryHandler` | `UserRepository`, `UserDto` | none |

## Workspace HTTP 유스케이스

| Use Case | Endpoint | Command/Query | Handler | 주요 domain/port | Event |
| --- | --- | --- | --- | --- | --- |
| CreateWorkspace | `POST /api/v1/workspaces` | `CreateWorkspaceCommand` | `CreateWorkspaceCommandHandler` | `TenantContract`, `UserContract`, `Workspace`, `WorkspaceMember`, `WorkspaceRepository`, `WorkspaceMemberRepository` | none |
| GetWorkspace | `GET /api/v1/workspaces/{workspaceId}` | `GetWorkspaceQuery` | `GetWorkspaceQueryHandler` | `WorkspaceRepository`, `WorkspaceDto` | none |

## HTTP Command 유스케이스

| Use Case | Endpoint | Command | Handler | 주요 domain/port | Event |
| --- | --- | --- | --- | --- | --- |
| SendMessage | `POST /api/v1/chat-rooms/{chatRoomId}/messages` | `SendMessageCommand` | `SendMessageCommandHandler` | `Message`, `MessageRepository`, `ChatRoomSequenceRepository` | `MessageSentEvent` |
| SendReply | `POST /api/v1/chat-rooms/{chatRoomId}/messages/{replyToMessageId}/replies` | `SendReplyCommand` | `SendReplyCommandHandler` | `Message`, `MessageRepository` | `MessageSentEvent` |
| EditMessage | `PATCH /api/v1/chat-rooms/{chatRoomId}/messages/{messageId}` | `EditMessageCommand` | `EditMessageCommandHandler` | `Message.edit`, `MessageRepository` | `MessageEditedEvent` |
| DeleteMessage | `DELETE /api/v1/chat-rooms/{chatRoomId}/messages/{messageId}` | `DeleteMessageCommand` | `DeleteMessageCommandHandler` | `Message.delete`, `MessageRepository` | `MessageDeletedEvent` |
| MarkRead | `PUT /api/v1/chat-rooms/{chatRoomId}/read-state` | `MarkReadCommand` | `MarkReadCommandHandler` | `ChatRoomMember.updateLastRead`, `MessageRepository` | `MessageReadEvent` |
| JoinRoom | `PUT /api/v1/chat-rooms/{chatRoomId}/members/me` | `JoinRoomCommand` | `JoinRoomCommandHandler` | `ChatRoomMember.create/rejoin` | `MemberJoinedEvent` |
| LeaveRoom | `DELETE /api/v1/chat-rooms/{chatRoomId}/members/me` | `LeaveRoomCommand` | `LeaveRoomCommandHandler` | `ChatRoomMember.leave` | `MemberLeftEvent` |
| CreateGroupRoom | `POST /api/v1/chat-rooms/groups` | `CreateGroupRoomCommand` | `CreateGroupRoomCommandHandler` | `RoomParticipantPolicy`, `ChatRoom.createGroup`, `ChatRoomCreationSupport` | none |
| CreateChannelRoom | `POST /api/v1/chat-rooms/channels` | `CreateChannelRoomCommand` | `CreateChannelRoomCommandHandler` | `RoomParticipantPolicy`, `ChatRoom.createChannel`, channel name uniqueness, `PermissionPort` | none |
| ScheduleRoomDeletion | `PUT /api/v1/chat-rooms/{chatRoomId}/deletion-schedule` | `ScheduleRoomDeletionCommand` | `ScheduleRoomDeletionCommandHandler` | `ChatRoom.scheduleForDeletion`, `PermissionPort` | none |
| DeleteRoomImmediately | `DELETE /api/v1/chat-rooms/{chatRoomId}` | `DeleteRoomImmediatelyCommand` | `DeleteRoomImmediatelyCommandHandler` | `ChatRoom.deleteImmediately`, `PermissionPort` | none |
| GetOrCreateDirectRoom | `POST /api/v1/chat-rooms/direct` | `GetOrCreateDirectRoomCommand` | `GetOrCreateDirectRoomCommandHandler` | `DirectRoomCreationService`, `DirectChatMapping` | none |

## HTTP Query 유스케이스

| Use Case | Endpoint | Query | Handler | 주요 port/output |
| --- | --- | --- | --- | --- |
| ListRooms | `GET /api/v1/chat-rooms` | `ListRoomsQuery` | `ListRoomsHandler` | `ChatRoomMemberReadRepository`, `ChatRoomReadRepository`, `MessageReadRepository`, `RoomDto` |
| ListMessages | `GET /api/v1/chat-rooms/{chatRoomId}/messages` | `ListMessagesQuery` | `ListMessagesHandler` | `MessageReadRepository`, `MessagePageDto` |
| ReadMessage | `GET /api/v1/chat-rooms/{chatRoomId}/messages/{messageId}` | `ReadMessageQuery` | `ReadMessageHandler` | `MessageReadRepository`, `MessageDto` |
| GetUnreadCount | `GET /api/v1/chat-rooms/{chatRoomId}/unread-count` | `GetUnreadCountQuery` | `GetUnreadCountHandler` | `MessageReadRepository`, `UnreadCountResult` |

## STOMP Command 유스케이스

| Use Case | Destination | Command | Handler | Output |
| --- | --- | --- | --- | --- |
| SendMessage | `/app/chat.message.send` | `SendMessageCommand` | `SendMessageCommandHandler` | `RoomEventEnvelope(type=MESSAGE_SENT)` to room event topic |
| EditMessage | `/app/chat.message.edit` | `EditMessageCommand` | `EditMessageCommandHandler` | `RoomEventEnvelope(type=MESSAGE_EDITED)` to room event topic |
| DeleteMessage | `/app/chat.message.delete` | `DeleteMessageCommand` | `DeleteMessageCommandHandler` | `RoomEventEnvelope(type=MESSAGE_DELETED)` to room event topic |
| TypingStart | `/app/chat.typing.start` | `SendTypingCommand(status=STARTED)` | `SendTypingCommandHandler` | `TypingEventPayload` to typing topic |
| TypingStop | `/app/chat.typing.stop` | `SendTypingCommand(status=STOPPED)` | `SendTypingCommandHandler` | `TypingEventPayload` to typing topic |

## 공통 접근 검증

- Send/typing: `ChatRoomAccessSupport.loadSendableActiveMember`
- Read/query: `ChatRoomReadAccessSupport.loadReadableActiveMember`
- Join: `ChatRoomAccessSupport.loadJoinableRoom` (`ACTIVE` public channel self-join only)
- Leave: `ChatRoomAccessSupport.loadLeavableMember`
- Edit/delete message: message sender 검증 전에 `chatRoomId` 포함 조회와 `ChatRoomAccessSupport.loadSendableActiveMember`를 다시 통과한다.
- Channel create/delete: `ChatRoomAuthorizationService`가 `PermissionPort`에 fine-grained permission을 질의한다.
- Room participant validation: `RoomParticipantPolicy`가 `RoomParticipantDirectoryPort`를 통해 requester와 target user의 tenant/workspace membership을 batch 검증한다.
- Tenant user existence: messenger infrastructure의 `ContractRoomParticipantDirectoryAdapter`는 `pabal-integration-contract`의 `UserContract`를 통해 user module의 active tenant user를 조회한다.
- Workspace membership: `ContractRoomParticipantDirectoryAdapter`는 `pabal-integration-contract`의 `WorkspaceContract`를 통해 workspace module의 active workspace member를 조회한다.

## 구현상 중요한 세부

- `RequestTenantRegistrationCommandHandler`는 domain을 lower-case canonical form으로 정규화하고 `_pabal-verification.{domain}` TXT record에 넣을 `pabal-verification={token}` 값을 발급한다.
- `VerifyTenantDomainCommandHandler`(verify-only)는 registration row를 pessimistic lock(`findByIdForUpdate`)으로 읽은 뒤 DNS TXT record를 확인하고, 성공하면 `markVerified(now, now+activationExpiresAt)`로 `DOMAIN_VERIFIED`로 전환해 persist한다. `activationExpiresAt`(= `now + pabal.tenant.registration.activation-window-ms`, 기본 604800000ms)을 함께 설정하지만 `Tenant`는 생성하지 않는다(`TenantRepository` 의존성 자체가 없다).
- `ActivateTenantRegistrationCommandHandler`는 `findByIdForUpdate` 락 이후 상태(`DOMAIN_VERIFIED`)와 window(`now < activationExpiresAt`)를 읽기 전용으로 먼저 검증해 fail-fast한 뒤, `Tenant.create` + `activate(tenantId, now)`로 `Tenant`를 정확히 1건 생성하고 registration을 `ACTIVATED`로 전환한다. 검증 실패 시 `Tenant`는 저장되지 않는다.
- `ReverifyTenantRegistrationCommandHandler`는 `validateReverificationAllowed()`로 DNS 조회 전에 상태(`REVERIFICATION_REQUIRED`)를 먼저 검증하고, 통과하면 기존 verification token으로 DNS TXT를 재확인한 뒤 `reverify(now, now+activation-window)`로 `DOMAIN_VERIFIED`로 복귀시킨다. 상태가 맞지 않으면 DNS 조회를 전혀 수행하지 않는다.
- `PollTenantDomainVerificationsCommandHandler`는 `PENDING_VERIFICATION` registration을 queue item처럼 읽어 (verify-only) `VerifyTenantDomainCommandHandler`를 호출해 DNS TXT verification을 자동 재시도한다. `verifiedCount`는 `DOMAIN_VERIFIED`로의 전이만 세며 activation은 절대 수행하지 않는다. infrastructure scheduler가 기본 600초 간격으로 호출하며, 직접 검증 trigger는 local/test dev endpoint로만 노출한다.
- `RenewTenantRegistrationTokenCommandHandler`는 `PENDING_VERIFICATION` registration의 verification token과 `verificationExpiresAt`을 새 값(= `now + pabal.tenant.registration.verification-window-ms`, 기본 604800000ms)으로 회전한다.
- `ExpireTenantRegistrationsCommandHandler`는 `verificationExpiresAt`이 지난 `PENDING_VERIFICATION` registration을 `EXPIRED`로 닫는다. infrastructure scheduler가 기본 600초 간격으로 호출하고, registration 새 요청 시에도 먼저 만료 sweep을 실행한다.
- `ReverifyLapsedTenantRegistrationsCommandHandler`는 `activationExpiresAt`이 지난 `DOMAIN_VERIFIED` registration을 도메인 메서드 `requireReverification(now)`를 통해서만 `REVERIFICATION_REQUIRED`로 전이한다(자세한 내용은 위 "TenantRegistration domain 전이 모델" 절 참고). infrastructure scheduler가 기본 600초 간격으로 호출한다.
- `TenantRegistrationRepository.expireLapsedReverificationRegistrations(Instant, long)`는 `activationExpiresAt + reverification-grace-ms <= now`인 `REVERIFICATION_REQUIRED` row를 bulk UPDATE로 `EXPIRED`로 닫는다. grace window는 `pabal.tenant.registration.reverification-grace-ms`(기본 604800000ms)이며, `TenantRegistrationExpirationScheduler.expirePendingRegistrations()`가 기존 pending expiry sweep과 함께 호출한다.
- `RequestTenantRegistrationCommandHandler`/`RenewTenantRegistrationTokenCommandHandler`의 verification window와 `VerifyTenantDomainCommandHandler`/`ActivateTenantRegistrationCommandHandler`/`ReverifyTenantRegistrationCommandHandler`의 activation window는 하드코딩된 상수가 아니라 `pabal.tenant.registration.verification-window-ms`/`activation-window-ms` 프로퍼티로 외부화되어 있다(둘 다 기본값 604800000ms = 7일).
- 위 handler들의 persistence 계층은 domain 전이 모델을 반영한다(`TenantRegistrationEntity`, `TenantRegistrationRepositoryImpl`). application의 verify/activate/reverify handler는 완전한 two-phase 흐름으로 분리되었고, API 응답도 새 status/두 timestamp를 노출한다. 현재 domain 전이 모델은 위 "TenantRegistration domain 전이 모델" 절과 [ADR-0013](../adr/0013-split-overloaded-expiry-timestamp-into-verification-and-activation-windows.md)을 참고한다.
- `SendMessageCommandHandler`와 `SendReplyCommandHandler`는 `clientMessageId` 기반 중복을 먼저 조회하고, race condition은 `uq_message_client_id` 제약과 `DuplicateMessageException` 재조회로 흡수한다.
- `GetOrCreateDirectRoomCommandHandler`는 기존 mapping을 먼저 조회하고, concurrent create race는 `DuplicateDirectChatMappingException` 후 재조회로 흡수한다.
- `MarkReadCommandHandler`는 cursor가 실제로 전진한 경우에만 `MessageReadEvent`를 발행한다.
- channel create/deletion 권한은 `MessengerPermission` 기준으로 분리한다. `RbacPermissionAdapter`는 persisted RBAC permission, tenant owner/admin, workspace owner/admin, channel owner role과 scoped permission authority를 application `PermissionPort`로 변환한다. workspace owner/admin은 JWT authority뿐 아니라 `workspace_member.role` 기준으로도 판정한다.
- access/refresh token lifecycle은 business command/query가 아니라 security cross-cutting flow다. `RefreshTokenService`가 opaque refresh token rotation과 access token 재발급을 담당한다.

## 같이 봐야 하는 문서

- endpoint 예시는 [Pabal HTTP API 예시와 오류 매핑](http-api-and-error-mapping.md)
- sequence diagram은 [Pabal 엔드포인트 시퀀스 다이어그램](endpoint-sequence-diagrams.md)
- realtime payload는 [Pabal Realtime 이벤트 스키마](../realtime/event-schema.md)
