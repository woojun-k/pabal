---
tags:
  - pabal
  - architecture
  - msa
  - checklist
---

# Pabal MSA 전환 준비 체크리스트

> 상위 문서: [Pabal 아키텍처 개요](overview.md)
> 관련 문서: [Pabal 멀티모듈 전환 전략](multi-module-transition.md), [Pabal 패키지 구조와 레이어](package-structure-and-layers.md), [Pabal Realtime 이벤트 스키마](../realtime/event-schema.md), [Pabal 보안과 JWT Claim 설계](../security/jwt-claim-design.md)

## 상태

Status: Proposed

Pabal은 현재 MSA가 아니다. 이 문서는 멀티모듈 모놀리스가 안정화된 이후 messenger bounded context를 독립 서비스로 분리할 수 있는지 판단하기 위한 체크리스트다.

## 분리 후보

### Messenger API/Application/Domain

분리 후보:

- HTTP command/query API
- STOMP room event/typing event
- messenger room/member/message persistence
- unread count/read cursor

분리 전 조건:

- command/query 외부 계약이 versioning 가능해야 한다.
- transaction boundary를 단일 DB transaction 밖으로 옮길 전략이 있어야 한다.
- domain event delivery가 in-process event에서 outbox/broker 기반으로 바뀌어야 한다.

### Realtime Gateway

분리 후보:

- STOMP endpoint
- subscription authorization
- room event fanout

분리 전 조건:

- `RoomEventEnvelope` schema versioning
- user control event contract 안정화
- room/member authorization 조회 API 또는 cache 전략

## 데이터 소유권

현재 데이터는 `pabal-app` Flyway migration의 단일 schema에 있다.

분리 시 messenger service가 소유해야 할 테이블 후보:

- `chat_room`
- `chat_room_member`
- `direct_chat_mapping`
- `message`

분리 전 확인:

- tenant/user/workspace 소유권이 외부 서비스와 어떻게 연결되는가?
- user profile 조회가 필요한 경우 sync API, event replication, cache 중 무엇을 쓸 것인가?
- unread count와 last message snapshot을 같은 transaction 안에서 유지할 것인가?

## 외부 계약 후보

- HTTP: `/api/v1/chat-rooms/**`
- STOMP inbound: `/app/chat.typing.start`, `/app/chat.typing.stop`
- STOMP outbound: `/topic/tenants/{tenantId}/chat-rooms/{chatRoomId}/events`
- STOMP typing: `/topic/tenants/{tenantId}/chat-rooms/{chatRoomId}/typing`
- User control: `/user/queue/chat.control`
- JWT claims: `uid`, `tenant_id`, `sub`, audience

## 분리 전 선행 조건

- [ ] 멀티모듈 의존 규칙 자동 검증
- [ ] 외부 API/Realtime payload versioning 정책
- [ ] outbox 또는 broker 기반 domain event publishing
- [ ] cross-service authorization model
- [ ] service-to-service authentication
- [ ] schema migration ownership 분리
- [ ] distributed tracing/log correlation 표준화
- [ ] local/test environment에서 독립 서비스 실행 방식 정리
- [ ] contract test와 consumer-driven test 도입

## 아직 분리하면 안 되는 이유

- `SpringDomainEventPublisher`는 in-process after-commit event에 의존한다.
- Flyway migration과 application resource가 `pabal-app`에 모여 있다.
- `pabal-common`, `pabal-security`가 아직 library 형태로 공유되는 전제다.
- room/member authorization은 DB 조회를 즉시 수행한다.
- realtime fanout과 application use case가 같은 배포 단위에서 동작한다.

## 판단 기준

MSA 분리는 다음 조건이 모두 충족될 때 검토한다.

- module boundary 위반이 자동으로 차단된다.
- messenger 데이터 소유권이 명확하다.
- 외부 계약과 event schema가 versioning 가능하다.
- 운영상 독립 배포의 이점이 단일 배포의 단순함보다 크다.
