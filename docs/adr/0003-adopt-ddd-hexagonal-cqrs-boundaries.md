# ADR-0003: Messenger는 DDD + Hexagonal + CQRS 경계를 따른다

## Status

Accepted

## Context

Pabal Messenger는 단순 CRUD보다 도메인 규칙과 접근 제어가 중요하다. 예를 들어 room type, member lifecycle, message sequence, direct room idempotency, channel deletion lifecycle, read cursor 등은 단순 테이블 조작만으로 표현하기 어렵다.

또한 HTTP command, HTTP query, STOMP command, realtime outbound가 함께 존재하므로 진입점과 유스케이스의 책임을 명확히 나눌 필요가 있다.

## Decision

Messenger 모듈은 DDD + Hexagonal Architecture + CQRS 경계를 따른다.

- Domain은 entity, value object, domain policy, domain event, domain exception을 소유한다.
- Application은 command/query handler, use case orchestration, transaction boundary, outbound port를 소유한다.
- API는 HTTP/STOMP 요청을 command/query로 변환하고 application에 위임한다.
- Infrastructure는 JPA adapter, STOMP adapter, WebSocket security, clock adapter 등 기술 구현을 담당한다.
- Command와 Query는 handler와 모델을 분리한다.

## Consequences

### Positive

- 비즈니스 규칙이 domain에 모여 변경 영향이 줄어든다.
- command/query 책임이 분리되어 읽기 최적화와 쓰기 규칙 검증을 독립적으로 다룰 수 있다.
- infrastructure 교체 또는 분리 가능성이 높아진다.
- 테스트 단위가 domain/application/api/infrastructure로 나뉜다.

### Negative

- 단순 CRUD보다 파일 수, 매핑 코드, 테스트 코드가 증가한다.
- 팀원이 구조를 이해하기 전까지 진입 비용이 있다.
- 작은 변경에도 여러 계층의 타입을 함께 수정해야 할 수 있다.

### Follow-up

- [ ] 신규 유스케이스 추가 시 command/query 분리 기준을 문서화한다.
- [ ] application handler와 domain policy의 책임 경계를 계속 점검한다.
- [ ] mapper 증가 비용을 줄이기 위한 일관된 패턴을 유지한다.
