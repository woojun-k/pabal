# ADR-0005: Realtime outbound는 after-commit domain event 기반으로 발행한다

## Status

Accepted

## Context

메시지 전송, 수정, 삭제, 읽음 처리 등은 DB 상태 변경과 realtime event 발행이 함께 발생한다. 트랜잭션이 rollback되었는데 WebSocket event가 먼저 발행되면 클라이언트가 실제 저장되지 않은 상태를 관측할 수 있다.

현재 Pabal은 같은 애플리케이션 프로세스 안에서 Spring application event를 사용하며, 외부 broker나 outbox 기반 durable delivery는 아직 구현하지 않는다.

## Decision

Realtime outbound event는 transaction commit 이후 발행한다.

- `DomainEventPublisher.publishAfterCommit`을 사용한다.
- command/service transaction 안에서 domain event 발행을 예약한다.
- application listener가 domain event를 realtime payload로 변환한다.
- `ChatRealtimePort`를 통해 infrastructure의 STOMP adapter로 전송한다.
- durable delivery가 필요한 시점에는 outbox/event broker 도입을 별도 ADR로 검토한다.

## Consequences

### Positive

- rollback된 변경에 대한 realtime event 발행을 방지한다.
- domain event와 realtime transport의 결합을 줄인다.
- application port를 통해 STOMP 외의 전송 방식으로 확장할 수 있다.

### Negative

- 현재 구조는 프로세스 내부 event이므로 durable delivery를 보장하지 않는다.
- commit 이후 listener 또는 WebSocket 전송 실패 시 재시도/보상이 제한적이다.
- transaction이 없는 곳에서 `publishAfterCommit`을 호출하면 실패한다.

### Follow-up

- [ ] event delivery 실패 관측 지표를 추가한다.
- [ ] outbox 도입 조건을 MSA 전환 체크리스트와 연결한다.
- [ ] listener mapping test를 보강한다.
