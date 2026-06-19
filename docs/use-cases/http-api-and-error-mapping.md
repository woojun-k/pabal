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

## Tenant endpoints

### CreateTenant

`POST /api/v1/tenants`

Request:

```json
{
  "name": "Acme"
}
```

Response:

```json
{
  "tenantId": "018f0000-0000-7000-8000-000000000101",
  "name": "Acme",
  "status": "ACTIVE",
  "createdAt": "2026-04-29T00:00:00Z"
}
```

정책:

- 생성된 tenant는 `ACTIVE` 상태로 시작한다.
- tenant id는 infrastructure JPA UUID v7 generator가 생성한다.

주요 오류:

- `CMN002 INVALID_INPUT`

### GetTenant

`GET /api/v1/tenants/{tenantId}` → `TenantResponse`

주요 오류:

- `TNT404001 TENANT_NOT_FOUND`

## User endpoints

### CreateMe

`POST /api/v1/users/me`

Request:

```json
{
  "name": "Alice"
}
```

Response:

```json
{
  "userId": "018f0000-0000-7000-8000-000000000001",
  "tenantId": "018f0000-0000-7000-8000-000000000101",
  "name": "Alice",
  "status": "ACTIVE",
  "createdAt": "2026-04-29T00:00:00Z"
}
```

정책:

- userId와 tenantId는 `PabalPrincipal`에서 가져온다.
- request body에는 name만 받는다.
- `CreateUserCommandHandler`는 `TenantContract.existsActiveTenant`로 active tenant를 확인한다.
- 이미 같은 tenant/user가 존재하면 중복 user로 거부한다.

주요 오류:

- `USR409001 DUPLICATE_USER`
- `CMN002 INVALID_INPUT`

### GetMe / GetUser

- `GET /api/v1/users/me` → `UserResponse`
- `GET /api/v1/users/{userId}` → `UserResponse`

Response:

```json
{
  "userId": "018f0000-0000-7000-8000-000000000001",
  "tenantId": "018f0000-0000-7000-8000-000000000101",
  "name": "Alice",
  "status": "ACTIVE",
  "createdAt": "2026-04-29T00:00:00Z",
  "updatedAt": "2026-04-29T00:00:00Z"
}
```

정책:

- `/users/me`는 principal의 userId를 조회한다.
- `/users/{userId}`는 principal tenant 범위에서 path userId를 조회한다.

주요 오류:

- `USR404001 USER_NOT_FOUND`

## Workspace endpoints

### CreateWorkspace

`POST /api/v1/workspaces`

Request:

```json
{
  "name": "Engineering"
}
```

Response:

```json
{
  "workspaceId": "018f0000-0000-7000-8000-000000000401",
  "tenantId": "018f0000-0000-7000-8000-000000000101",
  "name": "Engineering",
  "status": "ACTIVE",
  "ownerId": "018f0000-0000-7000-8000-000000000001",
  "createdAt": "2026-04-29T00:00:00Z"
}
```

정책:

- tenantId와 ownerId는 `PabalPrincipal`에서 가져온다.
- request body에는 name만 받는다.
- active tenant가 아니면 생성하지 않는다.
- owner는 같은 tenant의 active user여야 한다.
- 생성 직후 owner는 `workspace_member`에 `OWNER`, `ACTIVE`로 저장된다.

주요 오류:

- `CMN002 INVALID_INPUT`

### GetWorkspace

`GET /api/v1/workspaces/{workspaceId}` → `WorkspaceResponse`

정책:

- principal tenant 범위에서 workspace를 조회한다.

주요 오류:

- `WSP404001 WORKSPACE_NOT_FOUND`

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

정책:

- `EditMessage`와 동일하게 sender, active membership, room status를 재검증한다.
- 삭제 시 원문 `content`는 tombstone 값 `[deleted]`로 대체한다.
- 조회 응답도 `DELETED` 메시지의 content를 `[deleted]`로 마스킹한다.

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

정책:

- `participantIds`는 requester와 함께 tenant membership을 batch 검증한다.
- 초대 대상이 있으면 `messenger:room:invite` fine-grained permission이 필요하다.

주요 오류:

- `MSG403009 ROOM_INVITE_PERMISSION_DENIED`
- `MSG403010 ROOM_PARTICIPANT_NOT_INVITABLE`
- `CMN002 INVALID_INPUT`

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
- `participantIds`는 requester와 함께 workspace membership을 batch 검증한다.
- 초대 대상이 있으면 `messenger:channel:invite` fine-grained permission도 필요하다.
- `ROLE_TENANT_ADMIN`, `ROLE_PABAL_ADMIN`, `ROLE_WORKSPACE_ADMIN`은 RBAC adapter에서 channel create/invite permission으로 매핑된다.

주요 오류:

- `MSG403008 CHANNEL_PERMISSION_DENIED`
- `MSG403010 ROOM_PARTICIPANT_NOT_INVITABLE`
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

정책:

- requester와 participant가 서로 다른 사용자여야 한다.
- requester와 participant가 같은 tenant membership에 있는지 batch 검증한다.

주요 오류:

- `MSG400006 INVALID_DIRECT_CHAT_PARTICIPANTS`
- `MSG403010 ROOM_PARTICIPANT_NOT_INVITABLE`

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
      "content": "[deleted]",
      "status": "DELETED",
      "replyToMessageId": null,
      "createdAt": "2026-04-29T00:00:00Z",
      "updatedAt": "2026-04-29T00:02:00Z",
      "deletedAt": "2026-04-29T00:02:00Z"
    }
  ],
  "nextCursor": 42,
  "hasNext": true
}
```

정책:

- 삭제된 메시지도 pagination sequence 보존을 위해 목록에 포함될 수 있다.
- `status = DELETED`인 메시지의 content는 항상 `[deleted]`로 반환한다.

### ReadMessage / GetUnreadCount

- `GET /api/v1/chat-rooms/{chatRoomId}/messages/{messageId}` → `MessageResponse`
- `GET /api/v1/chat-rooms/{chatRoomId}/unread-count` → `{ "unreadCount": 3 }`

## 구현상 주의

- `ListMessages`의 `size`는 1~100 범위다.
- `SendMessageResponse`, `EditMessageResponse`, `DeleteMessageResponse`, `MessageResponse`는 room-local `sequence`를 외부 계약에 포함한다.
- `SendMessageRequest.content`와 `MessageContent`는 5000자를 허용하고 DB는 `TEXT`와 check constraint로 같은 정책을 유지한다.
