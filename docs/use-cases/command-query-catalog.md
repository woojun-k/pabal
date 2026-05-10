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

Pabal Messenger의 HTTP 계약은 `/api/v1` 아래의 리소스 중심 endpoint로 노출된다. API controller는 외부 request를 application command/query record로 변환하고, application handler가 유스케이스를 처리한다.

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
| CreateGroupRoom | `POST /api/v1/chat-rooms/groups` | `CreateGroupRoomCommand` | `CreateGroupRoomCommandHandler` | `ChatRoom.createGroup`, `ChatRoomCreationSupport` | none |
| CreateChannelRoom | `POST /api/v1/chat-rooms/channels` | `CreateChannelRoomCommand` | `CreateChannelRoomCommandHandler` | `ChatRoom.createChannel`, channel name uniqueness, `PermissionPort` | none |
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
| TypingStart | `/app/chat.typing.start` | `SendTypingCommand(status=STARTED)` | `SendTypingCommandHandler` | `TypingEventPayload` to typing topic |
| TypingStop | `/app/chat.typing.stop` | `SendTypingCommand(status=STOPPED)` | `SendTypingCommandHandler` | `TypingEventPayload` to typing topic |

## 공통 접근 검증

- Send/typing: `ChatRoomAccessSupport.loadSendableActiveMember`
- Read/query: `ChatRoomReadAccessSupport.loadReadableActiveMember`
- Join: `ChatRoomAccessSupport.loadJoinableRoom` (`ACTIVE` public channel self-join only)
- Leave: `ChatRoomAccessSupport.loadLeavableMember`
- Edit/delete message: message sender 검증 전에 `chatRoomId` 포함 조회와 `ChatRoomAccessSupport.loadSendableActiveMember`를 다시 통과한다.
- Channel create/delete: `ChatRoomAuthorizationService`가 `PermissionPort`에 fine-grained permission을 질의한다.

## 구현상 중요한 세부

- `SendMessageCommandHandler`와 `SendReplyCommandHandler`는 `clientMessageId` 기반 중복을 먼저 조회하고, race condition은 `uq_message_client_id` 제약과 `DuplicateMessageException` 재조회로 흡수한다.
- `GetOrCreateDirectRoomCommandHandler`는 기존 mapping을 먼저 조회하고, concurrent create race는 `DuplicateDirectChatMappingException` 후 재조회로 흡수한다.
- `MarkReadCommandHandler`는 cursor가 실제로 전진한 경우에만 `MessageReadEvent`를 발행한다.
- channel create/deletion 권한은 `MessengerPermission` 기준으로 분리한다. `RbacPermissionAdapter`는 tenant admin/workspace admin/channel owner role과 scoped permission authority를 application `PermissionPort`로 변환한다.

## 같이 봐야 하는 문서

- endpoint 예시는 [Pabal HTTP API 예시와 오류 매핑](http-api-and-error-mapping.md)
- sequence diagram은 [Pabal 엔드포인트 시퀀스 다이어그램](endpoint-sequence-diagrams.md)
- realtime payload는 [Pabal Realtime 이벤트 스키마](../realtime/event-schema.md)
