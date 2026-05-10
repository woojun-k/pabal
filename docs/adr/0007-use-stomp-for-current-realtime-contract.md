# ADR-0007: 현재 Realtime Contract는 WebSocket/STOMP를 기준으로 둔다

## Status

Accepted

## Context

Pabal Messenger는 room event, typing event, user control event를 클라이언트에 실시간으로 전달해야 한다. 현재 구현은 Spring WebSocket/STOMP와 simple broker/relay 전환 가능성을 기준으로 구성되어 있다.

Realtime 기능은 HTTP API와 달리 destination, payload envelope, subscription authorization, connection authentication이 함께 관리되어야 한다.

## Decision

현재 realtime contract는 WebSocket/STOMP를 기준으로 정의한다.

- inbound destination과 outbound destination을 문서화한다.
- room event는 envelope + typed payload 형태로 관리한다.
- typing event와 user control event는 별도 destination/payload로 관리한다.
- STOMP adapter는 infrastructure에 둔다.
- application은 `ChatRealtimePort`만 알고 STOMP 구현 세부를 알지 않는다.

## Consequences

### Positive

- 클라이언트 연동 지점이 destination과 payload 기준으로 명확해진다.
- application과 transport 구현이 분리된다.
- 이후 broker relay 또는 다른 realtime transport로 전환할 여지가 있다.

### Negative

- STOMP destination 규칙과 authorization 규칙을 함께 유지해야 한다.
- payload versioning이 없으면 클라이언트 호환성 문제가 생길 수 있다.
- WebSocket 보안 테스트가 부족하면 subscription 경계가 약해질 수 있다.

### Follow-up

- [ ] realtime contract versioning 도입 여부를 검토한다.
- [ ] STOMP CONNECT/SUBSCRIBE 보안 테스트를 보강한다.
- [ ] broker relay 전환 조건과 운영 지표를 문서화한다.
