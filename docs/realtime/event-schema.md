---
tags:
  - pabal
  - realtime
  - event
  - stomp
---

# Pabal Realtime 이벤트 스키마

> 상위 문서: [Pabal 상세 설계 허브](../design/design-hub.md)
> 관련 문서: [Pabal STOMP 연동 가이드](stomp-guide.md), [Pabal 엔드포인트 시퀀스 다이어그램](../use-cases/endpoint-sequence-diagrams.md), [Pabal 보안과 JWT Claim 설계](../security/jwt-claim-design.md), [Websocket 설정](websocket-configuration.md)

## 개요

Layer: Contract / Application / Infrastructure
Status: Implemented

Realtime 외부 payload는 `pabal-messenger-contract/src/main/java/com/polarishb/pabal/messenger/contract/realtime`에 있다. 전송 구현은 `StompChatRealtimeAdapter`가 담당한다.

## destination 맵

### inbound

| 목적 | Destination | Controller |
| --- | --- | --- |
| typing start | `/app/chat.typing.start` | `ChatRealtimeCommandController.typingStart` |
| typing stop | `/app/chat.typing.stop` | `ChatRealtimeCommandController.typingStop` |

### outbound

| 목적 | Destination | Payload |
| --- | --- | --- |
| room event | `/topic/tenants/{tenantId}/chat-rooms/{chatRoomId}/events` | `RoomEventEnvelope` |
| typing | `/topic/tenants/{tenantId}/chat-rooms/{chatRoomId}/typing` | `TypingEventPayload` |
| user control | `/user/queue/chat.control` | `RoomSubscriptionRevokedRealtimePayload` |

## room event envelope

```json
{
  "eventId": "uuid",
  "schemaVersion": 1,
  "type": "MESSAGE_SENT",
  "tenantId": "uuid",
  "chatRoomId": "uuid",
  "sequence": 42,
  "aggregateVersion": 7,
  "occurredAt": "2026-04-29T00:00:00Z",
  "payload": {}
}
```

Code:

- `RoomEventEnvelope`
- `RoomEventType`
- `RoomEventPayload`

`RoomEventPayload`는 sealed interface이며, envelope의 `payload`는 아래 payload record 중 하나로 고정한다. `schemaVersion`의 현재 값은 `RoomEventEnvelope.CURRENT_SCHEMA_VERSION = 1`이다. `sequence`는 room-local event ordering과 client gap detection의 기준이다.

`RoomEventType`:

- `MESSAGE_SENT`
- `MESSAGE_EDITED`
- `MESSAGE_DELETED`
- `MESSAGE_READ`
- `MEMBER_JOINED`
- `MEMBER_LEFT`

## payload 스키마

### MESSAGE_SENT

Code: `MessageSentRealtimePayload`

```json
{
  "messageId": "uuid",
  "chatRoomId": "uuid",
  "sequence": 42,
  "senderId": "uuid",
  "clientMessageId": "uuid",
  "content": "hello",
  "createdAt": "2026-04-29T00:00:00Z"
}
```

### MESSAGE_EDITED

Code: `MessageEditedRealtimePayload`

```json
{
  "messageId": "uuid",
  "chatRoomId": "uuid",
  "sequence": 42,
  "content": "edited",
  "updatedAt": "2026-04-29T00:01:00Z"
}
```

### MESSAGE_DELETED

Code: `MessageDeletedRealtimePayload`

```json
{
  "messageId": "uuid",
  "chatRoomId": "uuid",
  "sequence": 42,
  "deletedAt": "2026-04-29T00:02:00Z"
}
```

### MESSAGE_READ

Code: `MessageReadRealtimePayload`

```json
{
  "userId": "uuid",
  "chatRoomId": "uuid",
  "lastReadMessageId": "uuid",
  "sequence": 42,
  "readAt": "2026-04-29T00:03:00Z"
}
```

### MEMBER_JOINED

Code: `MemberJoinedRealtimePayload`

```json
{
  "userId": "uuid",
  "chatRoomId": "uuid",
  "sequence": 42,
  "joinedAt": "2026-04-29T00:00:00Z"
}
```

### MEMBER_LEFT

Code: `MemberLeftRealtimePayload`

```json
{
  "userId": "uuid",
  "chatRoomId": "uuid",
  "sequence": 42,
  "leftAt": "2026-04-29T00:10:00Z"
}
```

`MemberLeftEventListener`는 room event와 함께 user control revocation도 전송한다.

## typing payload

Code: `TypingEventPayload`

```json
{
  "userId": "uuid",
  "status": "STARTED",
  "occurredAt": "2026-04-29T00:00:00Z"
}
```

현재 `ChatRealtimeCommandController`는 문자열 상수 `STARTED`, `STOPPED`를 사용한다. domain enum `TypingStatus`도 존재한다.

## user control payload

Code: `RoomSubscriptionRevokedRealtimePayload`

```json
{
  "tenantId": "uuid",
  "chatRoomId": "uuid",
  "revokedAt": "2026-04-29T00:10:00Z"
}
```

전송 경로:

```text
MemberLeftEventListener
→ ChatRealtimePort.publishSubscriptionRevocation
→ StompChatRealtimeAdapter.convertAndSendToUser
→ /user/queue/chat.control
```

## 설계 포인트

- domain event와 realtime payload는 같은 객체가 아니다.
- realtime payload는 contract layer에 있고, listener가 domain event를 typed payload와 `RoomEventEnvelope`로 변환한다.
- client는 `eventId`로 중복 수신을 제거하고, `sequence`로 ordering/gap detection을 수행한다.
- outbound room event는 after-commit 이후 전송된다.
- typing은 DB 상태 변경 없이 realtime port를 직접 호출한다.
