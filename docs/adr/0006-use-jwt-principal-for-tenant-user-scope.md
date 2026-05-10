# ADR-0006: JWT Principal을 Tenant/User Scope의 기준으로 사용한다

## Status

Accepted

## Context

Pabal은 멀티테넌시를 전제로 한다. API와 realtime 요청에서 tenant/user 식별이 흔들리면 다른 테넌트 또는 다른 사용자의 room/message에 접근할 수 있는 위험이 생긴다.

현재 구조에서는 HTTP와 WebSocket 경계에서 인증된 principal을 만들고, application layer에서 room membership과 operation별 authorization을 확인한다.

## Decision

JWT claim을 `PabalPrincipal`로 변환하고, 이 principal의 `tenantId`, `userId`를 tenant/user scope의 기준으로 사용한다.

- HTTP controller는 principal에서 tenant/user를 추출해 command/query에 반영한다.
- STOMP CONNECT에서도 인증 principal을 구성한다.
- room/member 접근 검증은 application support 또는 infrastructure authorization interceptor에서 수행한다.
- repository query에는 tenant 조건을 포함해 tenant boundary를 유지한다.
- local/test profile의 dev token은 운영 인증 흐름과 구분한다.

## Consequences

### Positive

- tenant/user scope의 출처가 명확해진다.
- HTTP와 STOMP에서 동일한 principal 모델을 사용할 수 있다.
- application layer에서 operation별 접근 검증을 일관되게 수행할 수 있다.

### Negative

- 모든 controller/handler에서 principal-derived scope를 누락하지 않도록 주의해야 한다.
- tenant isolation을 애플리케이션 레벨에서 관리하므로 휴먼 에러 가능성이 남는다.
- DB 레벨 RLS 같은 추가 방어선은 별도 설계가 필요하다.

### Follow-up

- [ ] repository tenant predicate 누락을 검증하는 테스트 또는 정적 검사를 검토한다.
- [ ] WebSocket subscription authorization test를 보강한다.
- [ ] RLS 또는 infrastructure-level tenant guard 도입 여부를 별도 ADR로 검토한다.
