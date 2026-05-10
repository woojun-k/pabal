---
tags:
  - pabal
  - testing
  - strategy
---

# Pabal 테스트 전략

> 상위 문서: [Pabal 상세 설계 허브](../design/design-hub.md)
> 관련 문서: [Pabal 테스트 케이스 카탈로그](test-case-catalog.md), [Pabal 에러 코드와 예외 매핑표](../use-cases/error-code-exception-mapping.md), [Pabal 인가 경계와 멀티테넌시 체크포인트](../security/authorization-and-multitenancy.md), [Pabal 멀티모듈 전환 전략](../architecture/multi-module-transition.md), [Pabal 로컬 개발과 런타임 구성](../architecture/local-runtime.md), [Pabal 데이터베이스 스키마와 제약](../architecture/database-schema-and-constraints.md), [Pabal 이벤트 발행과 트랜잭션 경계](../architecture/event-and-transaction-boundary.md)

## 개요

현재 테스트는 멀티모듈 구조에 맞춰 `pabal-common`, `pabal-security`, `pabal-messenger-domain`, `pabal-messenger-application`, `pabal-messenger-api`, `pabal-messenger-infrastructure`, `pabal-app`로 분산되어 있다.

## 실행 환경

Layer: Testing / App

- 전체 테스트 실행: `scripts/run-test.sh`
- 환경 파일: `.env.test`
- Gradle 명령: `./gradlew test`
- test profile resource: `pabal-app/src/test/resources/application-test.yaml`
- test WebSocket endpoint: `/ws`
- PostgreSQL Testcontainers image: `postgres:18.3`
- Redis Testcontainers image: `redis:8.6-alpine`

로컬 실행과 test profile 차이는 [Pabal 로컬 개발과 런타임 구성](../architecture/local-runtime.md)을 기준으로 본다.

## 현재 테스트 파일 맵

### Common / App support

- `pabal-common/src/test/java/com/polarishb/pabal/common/api/GlobalExceptionHandlerTest.java`
- `pabal-app/src/test/java/com/polarishb/pabal/common/event/SpringDomainEventPublisherIntegrationTest.java`
- `pabal-app/src/test/java/com/polarishb/pabal/support/AbstractPostgresIntegrationTest.java`
- `pabal-app/src/test/java/com/polarishb/pabal/support/AbstractRedisIntegrationTest.java`

### Security

- `pabal-security/src/test/java/com/polarishb/pabal/security/authentication/PabalJwtAuthenticationTokenTest.java`
- `pabal-security/src/test/java/com/polarishb/pabal/security/authentication/PabalJwtAuthenticationConverterTest.java`
- `pabal-security/src/test/java/com/polarishb/pabal/security/context/SecurityContextCurrentAuthenticationProviderTest.java`

### Domain

- `ChatRoomTest`
- `ChatRoomMemberTest`
- `MessageTest`
- `DirectChatMappingTest`
- `ChannelNameTest`
- `ChannelSettingsTest`
- `MessageContentTest`
- `OptionalNameTest`
- `RoomNameTest`
- `RoomNameFormatterTest`

### Application

- `SendMessageCommandHandlerTest`
- `SendReplyCommandHandlerTest`
- `MarkReadCommandHandlerTest`
- `CreateGroupRoomCommandHandlerTest`
- `GetOrCreateDirectRoomCommandHandlerTest`
- `JoinRoomCommandHandlerTest`
- `CreateChannelRoomCommandHandlerTest`
- `EditMessageCommandHandlerTest`
- `DeleteMessageCommandHandlerTest`
- `ChatRoomAuthorizationServiceTest`
- `DirectRoomCreationServiceTest`
- `ListRoomsHandlerTest`
- `ReadMessageHandlerTest`
- `GetUnreadCountHandlerTest`
- `MemberLeftEventListenerTest`

### API / Infrastructure

- `ChatCommandControllerTest`
- `ChatQueryControllerTest`
- `MessageWriteRepositoryImplTest`
- `DirectChatMappingWriteRepositoryImplTest`
- `RbacPermissionAdapterTest`
- `StompChatRealtimeAdapterTest`
- `PabalApplicationTests`

## 추천 테스트 피라미드

1. Domain unit test
2. Application handler test
3. API controller slice/contract test
4. Infrastructure persistence/realtime adapter test
5. App integration test

## 레이어별 책임

### Domain Test

Layer: Domain

- entity/VO invariant
- 상태 전이
- 예외 발생 조건
- persistence 또는 Spring 없이 테스트

### Application Test

Layer: Application

- command/query orchestration
- repository port interaction
- transaction/event publication intent
- tenant/member access support 사용 여부
- permission port 호출과 권한 거부 예외

### API Test

Layer: API

- request validation
- authentication principal mapping
- command/query mapper 호출
- response shape
- error mapping
- `/api/v1` resource path와 `sequence` response contract

### Infrastructure Test

Layer: Infrastructure

- JPA Entity ↔ State 변환
- unique constraint translation
- optimistic locking
- native query와 tenant filter
- STOMP destination 전송
- security context adapter와 RBAC authority mapping

### Integration Test

Layer: App

- Spring Boot context
- Flyway migration
- after-commit event publish
- security/resource wiring
- Testcontainers service connection

## DB/Persistence 테스트 기준

Layer: Infrastructure / App

- Flyway migration이 모든 test DB에 적용되는지 확인한다.
- `ddl-auto: validate` 실패가 schema drift를 드러내도록 둔다.
- message content 1~5000 정책은 API/domain/DB 회귀 테스트로 보호한다.
- unique constraint 기반 idempotency는 application 선조회와 DB conflict translation을 모두 검증한다.
- tenant 조건이 포함된 FK/unique/read query를 우선 검증한다.

관련 schema 설명은 [Pabal 데이터베이스 스키마와 제약](../architecture/database-schema-and-constraints.md)에서 본다.

## 이벤트/Realtime 테스트 기준

Layer: Common / Application / Infrastructure

- `publishAfterCommit`은 실제 transaction 안에서만 호출 가능해야 한다.
- commit 이후 listener가 실행되는지 검증한다.
- listener는 contract payload와 `RoomEventEnvelope`를 만든다.
- STOMP adapter는 `ChatRealtimeDestinations` 기준 destination으로 전송한다.
- CONNECT 인증과 SUBSCRIBE 인가는 분리해서 테스트한다.

이벤트 경계는 [Pabal 이벤트 발행과 트랜잭션 경계](../architecture/event-and-transaction-boundary.md)를 기준으로 본다.

## 현재 기준 우선 보강 포인트

Status: Proposed

- WebSocket CONNECT 인증 실패/성공 테스트
- `RoomSubscriptionAuthorizationManager` tenant mismatch, inactive member, deleted room subscribe 실패 테스트
- message content 5000자 정책 회귀 테스트
- module dependency rule test 또는 Gradle plugin 검증
- private channel invite/admin approval flow test once that flow is implemented
- PostgreSQL 통합 테스트에서 channel immediate delete와 `deleted_at` consistency 고정
- 전체 `./gradlew test`가 멀티모듈 전환 후 통과하는지 CI에 고정
