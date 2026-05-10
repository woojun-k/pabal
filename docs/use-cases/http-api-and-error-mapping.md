---
tags:
  - pabal
  - api
  - http
---

# Pabal HTTP API 예시와 오류 매핑

> 상위 문서: [Pabal 상세 설계 허브](../design/design-hub.md)
> 관련 문서: [Pabal Command-Query 유스케이스 카탈로그](command-query-catalog.md), [Pabal 에러 코드와 예외 매핑표](error-code-exception-mapping.md), [Pabal 보안과 JWT Claim 설계](../security/jwt-claim-design.md), [Pabal 인가 경계와 멀티테넌시 체크포인트](../security/authorization-and-multitenancy.md)

## 공통 사항

Layer: API

- Base path: `/api/v1`
- 인증: Bearer JWT
- principal source: `PabalPrincipal(userId, tenantId, subject)`
- HTTP request body의 tenant/user 값은 신뢰하지 않는다. API mapper가 authentication에서 tenant/user를 추출해 command/query에 넣는다.
- 외부 HTTP 경로에는 내부 CQRS 구조인 `command`, `query`를 노출하지 않는다.

## 공통 에러 포맷

```json
{
  "timestamp": "2026-04-29T00:00:00Z",
  "status": 400,
  "code": "CMN002",
  "message": "잘못된 입력입니다",
  "path": "/api/v1/chat-rooms/{chatRoomId}/messages",
  "traceId": "...",
  "details": [
    { "field": "content", "reason": "must not be blank" }
  ]
}
```

## Message endpoints

### SendMessage

`POST /api/v1/chat-rooms/{chatRoomId}/messages`

Request:

```json
{
  "clientMessageId": "018f0000-0000-7000-8000-000000000001",
  "content": "hello"
}
```

Response:

```json
{
  "messageId": "018f0000-0000-7000-8000-000000000101",
  "sequence": 42,
  "clientMessageId": "018f0000-0000-7000-8000-000000000001",
  "createdAt": "2026-04-29T00:00:00Z",
  "duplicated": false
}
```

주요 오류:

- `MSG404001 CHAT_ROOM_NOT_FOUND`
- `MSG403001 MEMBER_NOT_IN_ROOM`
- `MSG403002 MEMBER_NOT_ACTIVE`
- `MSG403006 ROOM_OPERATION_NOT_ALLOWED`
- `CMN002 INVALID_INPUT`

### SendReply

`POST /api/v1/chat-rooms/{chatRoomId}/messages/{replyToMessageId}/replies`

Request:

```json
{
  "clientMessageId": "018f0000-0000-7000-8000-000000000002",
  "content": "reply"
}
```

Response shape는 `SendMessageResponse`와 동일하며 `sequence`를 포함한다.

추가 오류:

- `MSG404002 MESSAGE_NOT_FOUND`
- `MSG400001 INVALID_REPLY_TARGET`

### EditMessage

`PATCH /api/v1/chat-rooms/{chatRoomId}/messages/{messageId}`

Request:

```json
{
  "newContent": "edited"
}
```

Response:

```json
{
  "messageId": "018f0000-0000-7000-8000-000000000101",
  "sequence": 42,
  "content": "edited",
  "updatedAt": "2026-04-29T00:01:00Z"
}
```

정책:

- path의 `chatRoomId`와 `messageId`를 함께 사용해 메시지를 조회한다.
- requester는 message sender여야 한다.
- requester는 현재 해당 room의 active member여야 한다.
- room은 send 가능한 상태여야 한다.

주요 오류:

- `MSG404002 MESSAGE_NOT_FOUND`
- `MSG403001 MEMBER_NOT_IN_ROOM`
- `MSG403002 MEMBER_NOT_ACTIVE`
- `MSG403003 MESSAGE_EDIT_FORBIDDEN`
- `MSG403006 ROOM_OPERATION_NOT_ALLOWED`
- `MSG400005 MESSAGE_ALREADY_DELETED`

### DeleteMessage

`DELETE /api/v1/chat-rooms/{chatRoomId}/messages/{messageId}`

Response:

```json
{
  "messageId": "018f0000-0000-7000-8000-000000000101",
  "sequence": 42,
  "deletedAt": "2026-04-29T00:02:00Z"
}
```

정책은 `EditMessage`와 동일하게 sender, active membership, room status를 재검증한다.

주요 오류:

- `MSG404002 MESSAGE_NOT_FOUND`
- `MSG403001 MEMBER_NOT_IN_ROOM`
- `MSG403002 MEMBER_NOT_ACTIVE`
- `MSG403005 MESSAGE_DELETE_FORBIDDEN`
- `MSG403006 ROOM_OPERATION_NOT_ALLOWED`
- `MSG400005 MESSAGE_ALREADY_DELETED`

## Room state and membership endpoints

### MarkRead

`PUT /api/v1/chat-rooms/{chatRoomId}/read-state`

Request:

```json
{
  "lastReadMessageId": "018f0000-0000-7000-8000-000000000101"
}
```

Success: `204 No Content`

### Room membership

- `PUT /api/v1/chat-rooms/{chatRoomId}/members/me` → `204 No Content`
- `DELETE /api/v1/chat-rooms/{chatRoomId}/members/me` → `204 No Content`

Join 정책:

- self-join은 `ACTIVE` public channel만 허용한다.
- private channel, direct room, group room은 roomId를 알아도 직접 join할 수 없다.

주요 오류:

- `MSG409004 MEMBER_ALREADY_ACTIVE`
- `MSG403002 MEMBER_NOT_ACTIVE`
- `MSG403006 ROOM_OPERATION_NOT_ALLOWED`
- `MSG403007 ROOM_JOIN_FORBIDDEN`

## Room creation endpoints

### CreateGroupRoom

`POST /api/v1/chat-rooms/groups`

Request:

```json
{
  "participantIds": ["018f0000-0000-7000-8000-000000000201"],
  "roomName": "team"
}
```

Response:

```json
{
  "chatRoomId": "018f0000-0000-7000-8000-000000000301",
  "roomName": "team"
}
```

### CreateChannelRoom

`POST /api/v1/chat-rooms/channels`

Request:

```json
{
  "workspaceId": "018f0000-0000-7000-8000-000000000401",
  "channelName": "backend",
  "isPrivate": false,
  "description": "backend channel",
  "participantIds": ["018f0000-0000-7000-8000-000000000201"]
}
```

정책:

- `messenger:channel:create` fine-grained permission이 필요하다.
- `ROLE_TENANT_ADMIN`, `ROLE_PABAL_ADMIN`, `ROLE_WORKSPACE_ADMIN`은 RBAC adapter에서 이 permission으로 매핑된다.

주요 오류:

- `MSG403008 CHANNEL_PERMISSION_DENIED`
- `MSG409003 DUPLICATE_CHANNEL_NAME`
- `CMN002 INVALID_INPUT`

### GetOrCreateDirectRoom

`POST /api/v1/chat-rooms/direct`

Request:

```json
{
  "participantId": "018f0000-0000-7000-8000-000000000201",
  "roomName": null
}
```

Response:

```json
{
  "chatRoomId": "018f0000-0000-7000-8000-000000000501"
}
```

주요 오류:

- `MSG400006 INVALID_DIRECT_CHAT_PARTICIPANTS`

## Channel deletion endpoints

- `PUT /api/v1/chat-rooms/{chatRoomId}/deletion-schedule` → `204 No Content`
- `DELETE /api/v1/chat-rooms/{chatRoomId}` → `204 No Content`

권한:

- 삭제 예약 owner scope: `messenger:channel:delete:schedule:own`
- 삭제 예약 any scope: `messenger:channel:delete:schedule:any`
- 즉시 삭제 owner scope: `messenger:channel:delete:execute:own`
- 즉시 삭제 any scope: `messenger:channel:delete:execute:any`

주요 오류:

- `MSG400002 ROOM_CANNOT_BE_DELETED`
- `MSG400003 INVALID_ROOM_STATUS`
- `MSG400004 INVALID_ROOM_STATUS_TRANSITION`
- `MSG403004 ROOM_DELETE_FORBIDDEN`

## Query endpoints

### ListRooms

`GET /api/v1/chat-rooms`

Response:

```json
[
  {
    "roomId": "018f0000-0000-7000-8000-000000000301",
    "name": "team",
    "type": "GROUP",
    "status": "ACTIVE",
    "lastMessageId": "018f0000-0000-7000-8000-000000000101",
    "lastMessageAt": "2026-04-29T00:00:00Z",
    "unreadCount": 3,
    "joinedAt": "2026-04-29T00:00:00Z"
  }
]
```

### ListMessages

`GET /api/v1/chat-rooms/{chatRoomId}/messages?cursor={sequence}&size=50`

Response:

```json
{
  "messages": [
    {
      "messageId": "018f0000-0000-7000-8000-000000000101",
      "chatRoomId": "018f0000-0000-7000-8000-000000000301",
      "senderId": "018f0000-0000-7000-8000-000000000001",
      "clientMessageId": "018f0000-0000-7000-8000-000000000001",
      "sequence": 42,
      "content": "hello",
      "status": "ACTIVE",
      "replyToMessageId": null,
      "createdAt": "2026-04-29T00:00:00Z",
      "updatedAt": "2026-04-29T00:00:00Z",
      "deletedAt": null
    }
  ],
  "nextCursor": 42,
  "hasNext": true
}
```

### ReadMessage / GetUnreadCount

- `GET /api/v1/chat-rooms/{chatRoomId}/messages/{messageId}` → `MessageResponse`
- `GET /api/v1/chat-rooms/{chatRoomId}/unread-count` → `{ "unreadCount": 3 }`

## 구현상 주의

- `ListMessages`의 `size`는 1~100 범위다.
- `SendMessageResponse`, `EditMessageResponse`, `DeleteMessageResponse`, `MessageResponse`는 room-local `sequence`를 외부 계약에 포함한다.
- `SendMessageRequest.content`와 `MessageContent`는 5000자를 허용하고 DB는 `TEXT`와 check constraint로 같은 정책을 유지한다.
