---
tags:
  - pabal
  - onboarding
  - messenger
---

# Pabal Messenger 온보딩 가이드

> 상위 문서: [Pabal Wiki Home](../README.md)
> 관련 문서: [Pabal 아키텍처 개요](../architecture/overview.md), [Pabal 로컬 개발과 런타임 구성](../architecture/local-runtime.md), [Pabal 패키지 구조와 레이어](../architecture/package-structure-and-layers.md), [Pabal 런타임 흐름](../architecture/runtime-flow.md), [Pabal Command-Query 유스케이스 카탈로그](../use-cases/command-query-catalog.md)

대상: 새로 합류한 백엔드 엔지니어
목적: 현재 멀티모듈 코드베이스의 큰 그림과 첫 학습 순서를 잡는다.

## 1. 한 문장 요약

Pabal Messenger는 `pabal-app`이 모든 모듈을 조립해 단일 애플리케이션으로 배포하는 멀티모듈 모놀리스이며, messenger 도메인은 DDD + Hexagonal + CQRS + STOMP realtime 구조로 분리되어 있다.

## 2. 먼저 볼 파일

1. `settings.gradle.kts`
2. `gradle/libs.versions.toml`
3. `pabal-app/src/main/java/com/polarishb/pabal/PabalApplication.java`
4. `pabal-app/src/main/resources/application.yaml`
5. `pabal-app/src/main/resources/application-local.yaml`
6. `compose.local.yaml`
7. `pabal-app/src/main/resources/db/migration/V2__messenger_tables.sql`
8. `pabal-common/src/main/java/com/polarishb/pabal/common/api/GlobalExceptionHandler.java`
9. `pabal-common/src/main/java/com/polarishb/pabal/common/event/SpringDomainEventPublisher.java`
10. `pabal-messenger-api/src/main/java/com/polarishb/pabal/messenger/api/command/http/ChatCommandController.java`
11. `pabal-messenger-application/src/main/java/com/polarishb/pabal/messenger/application/command/handler/SendMessageCommandHandler.java`
12. `pabal-messenger-domain/src/main/java/com/polarishb/pabal/messenger/domain/model/entity/Message.java`
13. `pabal-messenger-infrastructure/src/main/java/com/polarishb/pabal/messenger/infrastructure/persistence/write/MessageWriteRepositoryImpl.java`
14. `pabal-messenger-infrastructure/src/main/java/com/polarishb/pabal/messenger/infrastructure/config/WebSocketBrokerConfig.java`

## 3. 모듈 지도

| 모듈 | Layer | 먼저 볼 패키지 | 역할 |
| --- | --- | --- | --- |
| `pabal-app` | App | `com.polarishb.pabal` | Spring Boot 실행, 모듈 조립, resource/migration 보관 |
| `pabal-common` | Common | `common.api`, `common.event`, `common.cqrs` | 공통 API error, event publisher, CQRS marker, UUID v7 |
| `pabal-security` | Security | `security.authentication`, `security.config` | JWT decoder/converter, `PabalPrincipal`, HTTP security |
| `pabal-messenger-domain` | Domain | `domain.model`, `domain.event`, `domain.exception` | 순수 도메인 모델, invariant, 도메인 이벤트 |
| `pabal-messenger-contract` | Contract | `contract.persistence`, `contract.realtime` | persistence/realtime 경계 shape |
| `pabal-messenger-application` | Application | `command.handler`, `query.handler`, `port.out` | 유스케이스 orchestration, outbound port |
| `pabal-messenger-api` | API | `api.command`, `api.query` | HTTP/STOMP controller와 mapper |
| `pabal-messenger-infrastructure` | Infrastructure | `persistence`, `realtime.ws`, `config` | JPA adapter, STOMP adapter, WebSocket security |

## 4. 로컬 실행부터 확인하기

먼저 [Pabal 로컬 개발과 런타임 구성](../architecture/local-runtime.md)을 보고 local/test profile 차이를 확인한다.

```text
scripts/run-local.sh
→ .env.local
→ ./gradlew bootRun --args='--spring.profiles.active=local'
```

```text
scripts/run-test.sh
→ .env.test
→ ./gradlew test
```

local WebSocket endpoint는 `/websocket`, test endpoint는 `/ws`다. STOMP 예시를 볼 때 profile 차이를 먼저 확인한다.

## 5. HTTP command 흐름으로 익히기

Layer: API → Application → Domain → Application Port → Infrastructure Adapter

코드 흐름:

```text
ChatCommandController
→ ChatCommandMapper
→ SendMessageCommand
→ SendMessageCommandHandler
→ ChatRoomAccessSupport
→ MessageSendSupport port
→ Message
→ MessageRepository
→ MessageSendSupportAdapter
→ MessageRepositoryImpl
→ MessageWriteRepositoryImpl
→ MessageEntity
```

읽을 때의 포인트:

- controller는 인증 객체와 request를 mapper에 넘긴다.
- mapper는 `PabalPrincipal`에서 `tenantId`, `userId`를 꺼내 command에 넣는다.
- handler는 접근 검증, 중복 메시지 조회, 메시지 생성을 조립하고, transaction이 필요한 저장/sequence/event 등록은 `MessageSendSupportAdapter`가 처리한다.
- domain은 메시지 상태 전이와 값 검증만 담당한다.
- repository port는 domain이 아니라 application에 있다.

## 6. HTTP query 흐름으로 익히기

Layer: API → Application → Application Port → Infrastructure Adapter → API

코드 흐름:

```text
ChatQueryController
→ ChatQueryMapper
→ ListMessagesQuery
→ ListMessagesHandler
→ ChatRoomReadAccessSupport
→ MessageReadRepository
→ MessageReadRepositoryImpl
→ MessageReadJpaRepository
→ MessageQueryMapper
→ MessagePageResponse
```

조회 경로는 read repository를 사용하고, room/member 접근 검증은 query handler에서도 수행한다.

## 7. Realtime 흐름으로 익히기

Inbound typing:

```text
STOMP CONNECT
→ StompConnectAuthenticationInterceptor
→ WebSocketAuthenticationManagerConfig
→ PabalJwtAuthenticationConverter
→ ChatRealtimeCommandController
→ SendTypingCommandHandler
→ ChatRealtimePort
→ StompChatRealtimeAdapter
```

Outbound room event:

```text
DomainEventPublisher.publishAfterCommit
→ MessageSentEventListener
→ RoomEventEnvelope
→ StompChatRealtimeAdapter
→ /topic/tenants/{tenantId}/chat-rooms/{chatRoomId}/events
```

자세한 transaction 경계는 [Pabal 이벤트 발행과 트랜잭션 경계](../architecture/event-and-transaction-boundary.md)에서 본다.

## 8. DB schema와 persistence 경계로 익히기

읽는 순서:

1. [Pabal 데이터베이스 스키마와 제약](../architecture/database-schema-and-constraints.md)에서 Flyway table/constraint를 확인한다.
2. [Pabal Persistence 경계와 데이터 변환](../architecture/persistence-boundary-and-mapping.md)에서 Domain Model ↔ State/Persisted ↔ JPA Entity 변환을 확인한다.
3. `MessageWriteRepositoryImpl`에서 DB unique constraint가 domain exception으로 번역되는 지점을 본다.

## 9. 흔한 실수

- `tenantId`를 request body나 path에서 신뢰하는 것. HTTP에서는 `PabalPrincipal` 기준으로 command/query를 만든다.
- domain에 JPA Entity, `MessageState`, `PersistedMessage`를 넣는 것.
- application handler에서 JPA repository를 직접 참조하는 것.
- API controller에 비즈니스 규칙을 넣는 것.
- WebSocket SUBSCRIBE authorization과 CONNECT authentication을 같은 책임으로 보는 것.
- read/write repository를 무의식적으로 혼용하는 것.
- local `/websocket` endpoint와 test `/ws` endpoint를 혼동하는 것.

## 10. 추천 학습 순서

1. [Pabal 아키텍처 개요](../architecture/overview.md)로 큰 구조를 잡는다.
2. [Pabal 로컬 개발과 런타임 구성](../architecture/local-runtime.md)으로 실행/profile 차이를 확인한다.
3. [Pabal 패키지 구조와 레이어](../architecture/package-structure-and-layers.md)에서 모듈/레이어 책임을 본다.
4. [Pabal 공통 모듈 설계](../architecture/common-module-design.md)에서 common에 둘 수 있는 것과 없는 것을 확인한다.
5. [Pabal 멀티모듈 전환 전략](../architecture/multi-module-transition.md)에서 의존 방향을 확인한다.
6. [Pabal 런타임 흐름](../architecture/runtime-flow.md)으로 HTTP/STOMP 흐름을 따라간다.
7. [Pabal 도메인 모델 상세](../domain/messenger-domain-model.md)로 entity/VO invariant를 확인한다.
8. [Pabal 데이터베이스 스키마와 제약](../architecture/database-schema-and-constraints.md)과 [Pabal Persistence 경계와 데이터 변환](../architecture/persistence-boundary-and-mapping.md)으로 DB/State/JPA 변환을 익힌다.
9. [Pabal 이벤트 발행과 트랜잭션 경계](../architecture/event-and-transaction-boundary.md)로 after-commit realtime 흐름을 확인한다.
10. [Pabal 보안과 JWT Claim 설계](../security/jwt-claim-design.md)와 [Pabal 인가 경계와 멀티테넌시 체크포인트](../security/authorization-and-multitenancy.md)로 tenant isolation을 확인한다.
11. [Pabal Observability와 운영 설정](../architecture/observability-and-operations.md)에서 trace/log/health 확인 지점을 본다.
12. [Pabal 테스트 전략](../testing/testing-strategy.md)에서 어느 레이어에 테스트를 추가할지 결정한다.

## 11. 첫 PR 체크리스트

상세 구현 backlog는 [Pabal 기술 부채와 보강 목록](../architecture/technical-debt.md)에 둔다.

- [ ] API request/response 변경이 필요한가?
- [ ] command/query input/output 변경이 필요한가?
- [ ] domain invariant 변경이 필요한가?
- [ ] persistence `State`/`Persisted*`/JPA Entity 변경이 필요한가?
- [ ] Flyway migration 또는 DB constraint 변경이 필요한가?
- [ ] authorization checkpoint가 있는가?
- [ ] realtime event/payload가 필요한가?
- [ ] after-commit event 발행이 필요한가?
- [ ] layer별 테스트 위치를 결정했는가?
- [ ] 관련 Obsidian 문서를 갱신했는가?
