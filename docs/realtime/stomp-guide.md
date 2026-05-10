---
tags:
  - pabal
  - realtime
  - stomp
  - guide
---

# Pabal STOMP 연동 가이드

> 상위 문서: [Pabal 상세 설계 허브](../design/design-hub.md)
> 관련 문서: [Pabal Realtime 이벤트 스키마](event-schema.md), [Pabal 보안과 JWT Claim 설계](../security/jwt-claim-design.md), [Pabal 인가 경계와 멀티테넌시 체크포인트](../security/authorization-and-multitenancy.md), [Websocket 설정](websocket-configuration.md), [Pabal 로컬 개발과 런타임 구성](../architecture/local-runtime.md), [Pabal 이벤트 발행과 트랜잭션 경계](../architecture/event-and-transaction-boundary.md)

## 접속 정보

Layer: Infrastructure / Security

- 기본/local endpoint: `/websocket`
- test profile endpoint: `/ws`
- application destination prefix: `/app`
- broker destination prefix: `/topic`, `/queue`
- SockJS: 설정값 `pabal.websocket.endpoint.sock-js-enabled`에 따라 활성화

Profile별 설정 차이는 [Pabal 로컬 개발과 런타임 구성](../architecture/local-runtime.md)에서 본다.

## CONNECT 인증

방법 1: Authorization header

```text
Authorization: Bearer {accessToken}
```

방법 2: access_token native header

```text
access_token: {accessToken}
```

처리 흐름:

```text
StompConnectAuthenticationInterceptor
→ WebSocketAuthenticationManagerConfig
→ JwtDecoder
→ PabalJwtAuthenticationConverter
→ PabalJwtAuthenticationToken(PabalPrincipal)
```

## SUBSCRIBE 인가

### room events

```text
/topic/tenants/{tenantId}/chat-rooms/{chatRoomId}/events
```

### typing

```text
/topic/tenants/{tenantId}/chat-rooms/{chatRoomId}/typing
```

인가 조건:

- authenticated principal이어야 한다.
- destination의 `tenantId`가 `PabalPrincipal.tenantId`와 같아야 한다.
- `ChatRoomReadRepository.findByTenantIdAndId`로 room이 존재해야 한다.
- room이 subscribe 가능한 상태여야 한다.
- `ChatRoomMemberRepository.findByTenantIdAndChatRoomIdAndUserId` 결과가 active member여야 한다.

### user control

```text
/user/queue/chat.control
```

authenticated user만 subscribe할 수 있다.

## SEND destination

### typing start

```text
SEND /app/chat.typing.start
```

Payload:

```json
{
  "tenantId": "uuid",
  "chatRoomId": "uuid"
}
```

### typing stop

```text
SEND /app/chat.typing.stop
```

Payload:

```json
{
  "tenantId": "uuid",
  "chatRoomId": "uuid"
}
```

주의:

- `tenantId`는 request payload에 있지만 그대로 신뢰하지 않는다.
- `ChatRealtimeCommandController.validateTenant`가 payload tenant와 principal tenant를 비교한다.
- 권한 검증은 `SendTypingCommandHandler`에서 `ChatRoomAccessSupport.loadSendableActiveMember`로 수행한다.

## room event 수신 예시

```json
{
  "eventId": "uuid",
  "schemaVersion": 1,
  "type": "MESSAGE_SENT",
  "tenantId": "uuid",
  "chatRoomId": "uuid",
  "sequence": 42,
  "aggregateVersion": 7,
  "payload": {
    "messageId": "uuid",
    "chatRoomId": "uuid",
    "sequence": 42,
    "senderId": "uuid",
    "clientMessageId": "uuid",
    "content": "hello",
    "createdAt": "2026-04-29T00:00:00Z"
  },
  "occurredAt": "2026-04-29T00:00:00Z"
}
```

room event는 application listener가 after-commit event를 realtime payload로 변환한 결과다. 상세 흐름은 [Pabal 이벤트 발행과 트랜잭션 경계](../architecture/event-and-transaction-boundary.md)에서 본다.

## typing event 수신 예시

```json
{
  "userId": "uuid",
  "status": "STARTED",
  "occurredAt": "2026-04-29T00:00:00Z"
}
```

## user control revocation 예시

```json
{
  "tenantId": "uuid",
  "chatRoomId": "uuid",
  "revokedAt": "2026-04-29T00:10:00Z"
}
```

## 클라이언트 체크리스트

- CONNECT에 bearer token을 포함한다.
- room topic subscribe 전에 해당 room member인지 확인한다.
- typing payload의 `tenantId`는 JWT tenant와 같은 값을 보낸다.
- user control queue를 구독해 강제 구독 해제 이벤트를 처리한다.
- `RoomEventEnvelope.type` 기준으로 payload를 분기한다.
- `eventId`로 중복 수신을 제거하고, `sequence`로 out-of-order와 gap을 판단한다.
