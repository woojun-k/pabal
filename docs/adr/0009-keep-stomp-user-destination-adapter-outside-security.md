# ADR-0009: STOMP user destination adapter는 security 밖에 둔다

Status: Accepted

Date: 2026-06-19

## Context

`PabalPrincipal`은 JWT claim을 `tenantId`, `userId`, `subject`로 정규화하는 security module의 principal이다. STOMP `/user` destination routing에는 Spring Messaging의 `DestinationUserNameProvider`가 필요하지만, 이를 `PabalPrincipal`의 public signature로 노출하면 `pabal-security`가 STOMP/WebSocket 세부사항에 의존하게 된다.

Security module은 HTTP/JWT 인증과 protocol-neutral principal 정규화에 집중해야 한다. STOMP user destination name은 messenger realtime infrastructure의 전송 세부사항이다.

## Decision

`PabalPrincipal`과 `PabalJwtAuthenticationToken`은 Spring Messaging 타입을 구현하지 않는다.

STOMP CONNECT 인증 후 messenger infrastructure에서 JWT authentication을 `StompAuthenticationToken`으로 감싸고, 이 adapter가 `DestinationUserNameProvider`를 구현한다. Destination user name은 `RealtimePrincipal.destinationUserName(tenantId, userId)` 규칙을 사용한다.

## Consequences

- `pabal-security`는 `spring-messaging` 의존성을 갖지 않는다.
- HTTP/JWT principal 모델과 STOMP `/user` routing adapter가 분리된다.
- STOMP CONNECT, SUBSCRIBE, `/user/queue/chat.control` 동작은 messenger infrastructure 테스트로 보호해야 한다.
- 다른 realtime protocol을 도입해도 security principal을 다시 바꾸지 않고 protocol adapter만 추가할 수 있다.

## Related

- [Pabal 보안과 JWT Claim 설계](../security/jwt-claim-design.md)
- [Pabal STOMP 연동 가이드](../realtime/stomp-guide.md)
- [Websocket 설정](../realtime/websocket-configuration.md)
