---
tags:
  - pabal
  - websocket
  - stomp
  - config
---

# Websocket 설정

> 상위 문서: [Pabal 상세 설계 허브](../design/design-hub.md)
> 관련 문서: [Pabal STOMP 연동 가이드](stomp-guide.md), [Pabal Realtime 이벤트 스키마](event-schema.md), [Pabal 보안과 JWT Claim 설계](../security/jwt-claim-design.md), [Pabal 인가 경계와 멀티테넌시 체크포인트](../security/authorization-and-multitenancy.md), [Pabal 로컬 개발과 런타임 구성](../architecture/local-runtime.md), [Pabal Observability와 운영 설정](../architecture/observability-and-operations.md)

## 엔드포인트 설정

Layer: Infrastructure

Code:

- `WebSocketBrokerConfig`
- `WebsocketEndpointProperties`
- `WebsocketRelayProperties`

설정:

- endpoint path: `pabal.websocket.endpoint.path`
- allowed origin patterns: `pabal.websocket.endpoint.allowed-origin-patterns`
- SockJS: `pabal.websocket.endpoint.sock-js-enabled`
- app prefix: `/app`
- broker: simple broker 또는 STOMP broker relay

기본/local 설정은 `/websocket`이고, test profile은 `/ws`를 사용한다. Profile별 런타임 차이는 [Pabal 로컬 개발과 런타임 구성](../architecture/local-runtime.md)에서 본다.

## Broker 설정

Layer: Infrastructure

`WebSocketBrokerConfig`는 항상 application destination prefix를 `/app`으로 설정한다.

- `pabal.websocket.relay.enabled=false`: `enableSimpleBroker("/topic", "/queue")`
- `pabal.websocket.relay.enabled=true`: `enableStompBrokerRelay("/topic", "/queue")`

relay 사용 시 host, port, login/passcode, virtual host, heartbeat 값을 `pabal.websocket.relay.*`로 설정한다.

## 인증 설정

Layer: Security / Infrastructure

Code:

- `StompConnectAuthenticationInterceptor`
- `WebSocketAuthenticationManagerConfig`
- `PabalJwtAuthenticationConverter`

CONNECT native header에서 token을 읽는다.

- `Authorization: Bearer {token}`
- `access_token: {token}`

인증 성공 시 STOMP accessor user가 `PabalJwtAuthenticationToken`으로 설정된다.

## 메시지 인가

Layer: Infrastructure / Application Port

Code:

- `StompMessageAuthorizationConfig`
- `RoomSubscriptionAuthorizationManager`

인가 규칙:

- `/app/**`: authenticated
- `/user/queue/chat.control`: authenticated subscribe
- `/topic/tenants/{tenantId}/chat-rooms/{chatRoomId}/events`: room member subscribe check
- `/topic/tenants/{tenantId}/chat-rooms/{chatRoomId}/typing`: room member subscribe check
- 기타 MESSAGE/SUBSCRIBE: deny

## 포트 및 어댑터

Layer: Application / Infrastructure

- Port: `ChatRealtimePort`
- Adapter: `StompChatRealtimeAdapter`
- Destination helper: `ChatRealtimeDestinations`

메서드:

- `publishRoomEvent`
- `publishTyping`
- `publishSubscriptionRevocation`

## 이벤트 리스너

Layer: Application

- `MessageSentEventListener`
- `MessageEditedEventListener`
- `MessageDeletedEventListener`
- `MessageReadEventListener`
- `MemberJoinedEventListener`
- `MemberLeftEventListener`

도메인 이벤트는 after-commit 이후 application listener가 realtime payload로 변환한다. 상세 경계는 [Pabal 이벤트 발행과 트랜잭션 경계](../architecture/event-and-transaction-boundary.md)에서 본다.

## 관측 포인트

Layer: Observability

local profile은 WebSocket, messaging, security 관련 로그를 DEBUG/TRACE로 높인다. 장애 조사 시 CONNECT 인증 로그와 SUBSCRIBE 인가 로그를 분리해서 확인한다. 상세 내용은 [Pabal Observability와 운영 설정](../architecture/observability-and-operations.md)에서 본다.
