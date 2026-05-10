---
tags:
  - pabal
  - architecture
  - backlog
  - technical-debt
---

# Pabal 기술 부채와 보강 목록

> 상위 문서: [Pabal 상세 설계 허브](../design/design-hub.md)
> 관련 문서: [Pabal 아키텍처 개요](overview.md), [Pabal 멀티모듈 전환 전략](multi-module-transition.md), [Pabal MSA 전환 준비 체크리스트](msa-readiness-checklist.md), [Pabal 테스트 전략](../testing/testing-strategy.md), [Pabal 인가 경계와 멀티테넌시 체크포인트](../security/authorization-and-multitenancy.md), [Pabal 데이터베이스 스키마와 제약](database-schema-and-constraints.md), [Pabal 이벤트 발행과 트랜잭션 경계](event-and-transaction-boundary.md)

## 목적

현재 코드베이스를 읽으면서 확인된 구현 보강 후보를 모은다. 이 문서는 wiki 항목 목록이 아니라 구현 backlog 성격이다. 위키 문서 항목은 [Pabal Wiki Home](../README.md), [Pabal 상세 설계 허브](../design/design-hub.md), [Pabal Messenger 온보딩 가이드](../onboarding/messenger-onboarding.md)에서 관리한다.

## 현재 정합성 메모

Status: Implemented

이전 문서에서 보강 후보로 다뤘던 메시지 길이 정책은 현재 migration 기준으로 정렬되어 있다.

- API request: `@Size(max = 5000)`
- Domain: `MessageContent.MAX_LENGTH = 5000`
- DB: `message.content TEXT NOT NULL` + `chk_message_content_length`

상세 내용은 [Pabal 데이터베이스 스키마와 제약](database-schema-and-constraints.md)과 [Pabal Persistence 경계와 데이터 변환](persistence-boundary-and-mapping.md)에서 본다.

## 우선순위 요약

| 우선순위 | 항목 | 상태 | 관련 문서 |
| --- | --- | --- | --- |
| P1 | WebSocket 보안 테스트 보강 | Proposed | [Pabal STOMP 연동 가이드](../realtime/stomp-guide.md), [Pabal 테스트 전략](../testing/testing-strategy.md) |
| P1 | module boundary 자동 검증 | Planned | [Pabal 멀티모듈 전환 전략](multi-module-transition.md), [Pabal 패키지 구조와 레이어](package-structure-and-layers.md) |
| P1 | workspace/channel 권한 source 정합화 | Planned | [Pabal 인가 경계와 멀티테넌시 체크포인트](../security/authorization-and-multitenancy.md) |
| P2 | TypingStatus enum과 STOMP typing 구현 정렬 | Proposed | [Pabal Realtime 이벤트 스키마](../realtime/event-schema.md) |
| P2 | unused realtime security 타입 정리 | Proposed | [Websocket 설정](../realtime/websocket-configuration.md), [Pabal 보안과 JWT Claim 설계](../security/jwt-claim-design.md) |
| P2 | Persistence 테스트 확장 | Proposed | [Pabal 테스트 케이스 카탈로그](../testing/test-case-catalog.md), [Pabal 데이터베이스 스키마와 제약](database-schema-and-constraints.md) |
| P2 | realtime event delivery 고도화 | Planned | [Pabal Realtime 이벤트 스키마](../realtime/event-schema.md), [Pabal MSA 전환 준비 체크리스트](msa-readiness-checklist.md) |
| P3 | outbox/event delivery 검토 | Planned | [Pabal 이벤트 발행과 트랜잭션 경계](event-and-transaction-boundary.md), [Pabal MSA 전환 준비 체크리스트](msa-readiness-checklist.md) |
| P3 | 첫 PR 체크리스트 보강 | Planned | [Pabal Messenger 온보딩 가이드](../onboarding/messenger-onboarding.md) |

## 1. TypingStatus enum과 STOMP typing 구현 정렬

Status: Proposed
Layer: Domain / API / Contract

현재 확인된 상태:

- `TypingStatus` enum은 domain에 있다.
- `ChatRealtimeCommandController`는 `"STARTED"`, `"STOPPED"` 문자열 상수를 직접 사용한다.
- `TypingEventPayload.status`는 `String`이다.

선택지:

- contract payload를 string으로 유지하고 controller 상수만 enum 기반으로 바꾼다.
- `TypingEventPayload.status`를 enum으로 바꾸고 JSON serialization 계약을 명시한다.
- domain enum이 실제 domain invariant가 아니라 contract enum이라면 contract layer로 옮긴다.

권장 방향:

- 외부 payload 안정성을 우선하면 `TypingEventPayload.status`는 string을 유지하고, 내부 생성만 `TypingStatus.name()`으로 정렬한다.

## 2. unused realtime security 타입 정리

Status: Proposed
Layer: Infrastructure / Security

현재 확인된 상태:

- `RealtimeAccessTokenAuthenticator`는 참조되지 않는다.
- `RealtimePrincipal`은 참조되지 않는다.
- 현재 STOMP 인증은 `StompConnectAuthenticationInterceptor` + `WebSocketAuthenticationManagerConfig` + `PabalJwtAuthenticationConverter` 흐름이다.

선택지:

- 사용하지 않는 타입이면 삭제한다.
- Realtime Gateway 분리 준비 타입이면 `Status: Planned`로 문서화하고 실제 사용 계획을 남긴다.

검증 방법:

- `rg "RealtimeAccessTokenAuthenticator|RealtimePrincipal"`로 참조 여부 확인.
- 삭제 시 WebSocket 인증 테스트가 현재 흐름을 보호해야 한다.

## 3. module boundary 자동 검증

Status: Planned
Layer: Architecture / Testing

필요한 이유:

- 멀티모듈 구조는 이미 구현됐지만, 금지 의존은 코드 리뷰만으로 유지하기 어렵다.
- 특히 `application → infrastructure`, `domain → contract`, `api → infrastructure`는 컴파일 또는 테스트 수준에서 막아야 한다.

검증 후보:

- ArchUnit test
- Gradle dependency analysis
- module convention plugin

최소 규칙:

```text
domain must not depend on contract/infrastructure/api
application must not depend on infrastructure
api must not depend on infrastructure
contract must not depend on infrastructure
common/security must not depend on messenger-*
```

## 4. WebSocket 보안 테스트 보강

Status: Proposed
Layer: Security / Infrastructure / Testing

보강 대상:

- `StompConnectAuthenticationInterceptor`
- `WebSocketAuthenticationManagerConfig`
- `StompMessageAuthorizationConfig`
- `RoomSubscriptionAuthorizationManager`

필수 테스트:

- CONNECT token 누락 시 실패
- CONNECT invalid token 시 실패
- CONNECT valid token 시 `PabalPrincipal` 설정
- SUBSCRIBE tenant mismatch deny
- SUBSCRIBE inactive member deny
- SUBSCRIBE missing/deleted/non-subscribable room deny
- SUBSCRIBE active member grant
- `/app/**`는 authenticated only
- unknown MESSAGE/SUBSCRIBE deny

## 5. workspace/channel 권한 source 정합화

Status: Partially Implemented
Layer: Application / Security / Infrastructure

현재 확인된 상태:

- `ChatRoomAuthorizationService`는 channel create와 channel deletion을 fine-grained `MessengerPermission`으로 판정한다.
- `PermissionPort`는 application boundary에 있고, `RbacPermissionAdapter`가 JWT authority를 permission으로 변환한다.
- tenant admin/pabal admin은 모든 Messenger permission, workspace admin은 channel create와 any deletion, channel owner는 own deletion을 가진다.

결정해야 할 정책:

- workspace admin/channel owner role의 source of truth를 JWT claim으로 유지할 것인가, workspace membership 조회 port로 옮길 것인가?
- room-scoped permission과 workspace-scoped permission을 운영 IAM에서 어떤 claim 형태로 발급할 것인가?
- private channel invite/admin approval 정책을 join flow에 어떻게 연결할 것인가?
- PostgreSQL RLS를 적용한다면 request tenant context를 DB session에 어떻게 주입할 것인가?

권장 분리:

- room 상태 전이 가능 여부는 domain `ChatRoom`이 검증한다.
- requester 권한 판정은 application policy/service에서 수행한다.
- JWT authority만으로 부족하면 workspace membership/role 조회 port가 필요하다.

## 6. realtime event delivery 고도화

Status: Partially Implemented
Layer: Contract / Realtime / Infrastructure

현재 확인된 상태:

- `RoomEventEnvelope`는 `eventId`, `schemaVersion`, `tenantId`, `chatRoomId`, `sequence`, `aggregateVersion`, `occurredAt`, typed `RoomEventPayload`를 가진다.
- message/member/read realtime payload에도 room-local `sequence`가 포함된다.
- 아직 durable outbox, broker delivery, correlation id는 없다.

다음 고도화 후보:

```json
{
  "eventId": "uuid",
  "schemaVersion": 1,
  "type": "MESSAGE_SENT",
  "tenantId": "uuid",
  "chatRoomId": "uuid",
  "sequence": 42,
  "aggregateVersion": 7,
  "occurredAt": "2026-04-29T00:00:00Z"
}
```

주의:

- envelope 필드 추가/삭제는 client contract migration이 필요하다.
- outbox나 외부 broker를 도입하면 [Pabal Realtime 이벤트 스키마](../realtime/event-schema.md)와 [Pabal STOMP 연동 가이드](../realtime/stomp-guide.md)를 함께 갱신해야 한다.

## 7. outbox/event delivery 검토

Status: Planned
Layer: Application / Infrastructure / MSA Readiness

현재 확인된 상태:

- `SpringDomainEventPublisher.publishAfterCommit`은 in-process after-commit 이벤트를 발행한다.
- listener도 같은 애플리케이션 프로세스 안에서 실행된다.

한계:

- process crash 이후 realtime event 재전송이 어렵다.
- 외부 broker로 durable delivery를 보장하지 않는다.
- MSA 분리 시 in-process event는 서비스 간 이벤트 계약이 될 수 없다.

검토 시점:

- realtime event 손실 허용 범위가 낮아질 때
- notification/audit/search projection 같은 consumer가 늘어날 때
- messenger를 독립 서비스로 분리할 때

## 8. Persistence 테스트 확장

Status: Proposed
Layer: Infrastructure / Testing

보강 후보:

- `ChatRoomSequenceRepositoryImpl.allocateNextMessageSequence` 동시성 테스트
- `MessageReadRepositoryImpl.countUnreadByRooms` native query tenant filter 테스트
- `uq_chat_room_channel_name_alive` 대소문자/삭제 상태 테스트
- `uq_direct_chat_mapping` 동시 생성 race 테스트
- optimistic locking version mismatch 테스트
- `MessageEntity`/`ChatRoomEntity` state round-trip 테스트
- message content 1~5000 정책 회귀 테스트

우선순위:

1. unread native query tenant filter
2. direct mapping race
3. room sequence allocation
4. message content boundary regression

## 9. 첫 PR 체크리스트 보강

Status: Planned
Layer: Onboarding / Testing / Documentation

신규 기능 추가 시 확인 순서:

- [ ] API request/response가 필요한가?
- [ ] command/query input/output이 필요한가?
- [ ] handler가 어떤 support/service와 port를 호출하는가?
- [ ] domain invariant 변경이 필요한가?
- [ ] persistence `State`/`Persisted*`/JPA Entity 변경이 필요한가?
- [ ] Flyway migration 또는 DB constraint 변경이 필요한가?
- [ ] repository port와 adapter를 모두 갱신했는가?
- [ ] realtime event 또는 payload가 필요한가?
- [ ] `MessengerErrorCode`와 예외 매핑이 필요한가?
- [ ] tenant/user authorization checkpoint가 있는가?
- [ ] domain/application/api/infrastructure 테스트를 어디에 둘지 결정했는가?
- [ ] 관련 Obsidian 문서를 갱신했는가?
