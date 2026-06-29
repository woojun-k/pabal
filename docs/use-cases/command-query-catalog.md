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
| VerifyTenantDomain | scheduler internal, dev `POST /dev/tenant-registrations/{registrationId}/domain-verification` | `VerifyTenantDomainCommand` | `VerifyTenantDomainCommandHandler` | `TenantRegistration`, `DnsTxtLookupPort`, `Tenant`, `TenantRepository` | none |

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
- Tenant user existence: messenger infrastructure의 `ContractRoomParticipantDirectoryAdapter`는 common `UserContract`를 통해 user module의 active tenant user를 조회한다.
- Workspace membership: `ContractRoomParticipantDirectoryAdapter`는 common `WorkspaceContract`를 통해 workspace module의 active workspace member를 조회한다.

## 구현상 중요한 세부

- `RequestTenantRegistrationCommandHandler`는 domain을 lower-case canonical form으로 정규화하고 `_pabal-verification.{domain}` TXT record에 넣을 `pabal-verification={token}` 값을 발급한다.
- `VerifyTenantDomainCommandHandler`는 registration row를 pessimistic lock으로 읽은 뒤 DNS TXT record를 확인하고, 성공하면 tenant를 생성한 뒤 registration을 `ACTIVATED`로 전환한다.
- `PollTenantDomainVerificationsCommandHandler`는 `PENDING_VERIFICATION` registration을 queue item처럼 읽어 DNS TXT verification을 자동 재시도한다. infrastructure scheduler가 기본 600초 간격으로 호출하며, 직접 검증 trigger는 local/test dev endpoint로만 노출한다.
- `RenewTenantRegistrationTokenCommandHandler`는 `PENDING_VERIFICATION` registration의 verification token과 `expiresAt`을 새 값으로 회전한다.
- `ExpireTenantRegistrationsCommandHandler`는 만료된 `PENDING_VERIFICATION` registration을 `EXPIRED`로 닫는다. infrastructure scheduler가 기본 600초 간격으로 호출하고, registration 새 요청 시에도 먼저 만료 sweep을 실행한다.
- `SendMessageCommandHandler`와 `SendReplyCommandHandler`는 `clientMessageId` 기반 중복을 먼저 조회하고, race condition은 `uq_message_client_id` 제약과 `DuplicateMessageException` 재조회로 흡수한다.
- `GetOrCreateDirectRoomCommandHandler`는 기존 mapping을 먼저 조회하고, concurrent create race는 `DuplicateDirectChatMappingException` 후 재조회로 흡수한다.
- `MarkReadCommandHandler`는 cursor가 실제로 전진한 경우에만 `MessageReadEvent`를 발행한다.
- channel create/deletion 권한은 `MessengerPermission` 기준으로 분리한다. `RbacPermissionAdapter`는 persisted RBAC permission, tenant owner/admin, workspace owner/admin, channel owner role과 scoped permission authority를 application `PermissionPort`로 변환한다. workspace owner/admin은 JWT authority뿐 아니라 `workspace_member.role` 기준으로도 판정한다.
- access/refresh token lifecycle은 business command/query가 아니라 security cross-cutting flow다. `RefreshTokenService`가 opaque refresh token rotation과 access token 재발급을 담당한다.

## 같이 봐야 하는 문서

- endpoint 예시는 [Pabal HTTP API 예시와 오류 매핑](http-api-and-error-mapping.md)
- sequence diagram은 [Pabal 엔드포인트 시퀀스 다이어그램](endpoint-sequence-diagrams.md)
- realtime payload는 [Pabal Realtime 이벤트 스키마](../realtime/event-schema.md)
