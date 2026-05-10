---
tags:
  - pabal
  - architecture
  - crosscutting
---

# Pabal 크로스커팅 관심사

> 상위 문서: [Pabal 아키텍처 개요](overview.md)
> 관련 문서: [Pabal 패키지 구조와 레이어](package-structure-and-layers.md), [Pabal 런타임 흐름](runtime-flow.md), [Pabal 보안과 JWT Claim 설계](../security/jwt-claim-design.md), [Pabal 인가 경계와 멀티테넌시 체크포인트](../security/authorization-and-multitenancy.md), [Pabal 공통 모듈 설계](common-module-design.md), [Pabal 이벤트 발행과 트랜잭션 경계](event-and-transaction-boundary.md), [Pabal Observability와 운영 설정](observability-and-operations.md)

## 1. 멀티테넌시

Layer: Security → API → Application → Infrastructure

- `PabalJwtAuthenticationConverter`가 JWT claim에서 `tenantId`, `userId`, `subject`를 추출한다.
- `PabalPrincipal`이 HTTP와 STOMP 인증 컨텍스트의 기준이다.
- API mapper는 principal의 tenant/user를 command/query에 넣는다.
- repository port와 adapter 메서드는 `findByTenantId...` 형태로 tenant 조건을 명시한다.
- STOMP subscription은 destination의 `{tenantId}`와 principal tenant가 일치해야 허용한다.

## 2. 인증과 인가

Layer: Security

HTTP:

- `SecurityConfig`는 `/actuator/health`, WebSocket endpoint, `/dev/*`를 제외한 요청을 인증 요구로 둔다.
- JWT resource server는 `JwtDecoder`와 `PabalJwtAuthenticationConverter`를 사용한다.
- local/test profile은 `LocalJwtConfig`, 운영 profile은 `IssuerJwtDecoderConfig`를 사용한다.

WebSocket/STOMP:

- handshake endpoint는 HTTP security에서 permitAll 처리된다.
- STOMP CONNECT에서 `StompConnectAuthenticationInterceptor`가 bearer token을 인증한다.
- SUBSCRIBE authorization은 `StompMessageAuthorizationConfig`와 `RoomSubscriptionAuthorizationManager`가 담당한다.

## 3. 예외 처리

Layer: Common

- `GlobalExceptionHandler`가 `GlobalException`을 `ApiError`로 변환한다.
- validation 예외는 `CMN002 INVALID_INPUT` 기준의 detail 목록으로 정규화한다.
- `AccessDeniedException`은 `CMN004 FORBIDDEN`으로 정규화한다.
- optimistic locking, data integrity violation은 conflict 응답으로 정규화한다.
- domain 예외는 `MessengerErrorCode`를 통해 public code/message/status를 가진다.
- `ApiError.traceId`는 OpenTelemetry Span 또는 MDC에서 얻는다.

## 4. 이벤트 발행

Layer: Common → Application → Infrastructure

- `SpringDomainEventPublisher.publishAfterCommit`은 실제 transaction이 있을 때만 after-commit synchronization을 등록한다.
- 메시지/멤버 이벤트 listener는 application layer에 있다.
- listener는 contract realtime payload를 만들고 `ChatRealtimePort`를 호출한다.
- STOMP 전송 구현은 `StompChatRealtimeAdapter`에 격리된다.
- 상세 경계는 [Pabal 이벤트 발행과 트랜잭션 경계](event-and-transaction-boundary.md)에서 본다.

## 5. ID와 시간

Layer: Common / Application / Infrastructure

- Java/JPA ID 생성은 `UuidV7IdGenerator`와 `@UuidV7Generated`를 사용한다.
- DB에는 `uuidv7()` 함수가 fallback/default로 준비되어 있다.
- application은 `ClockPort`를 통해 시간을 얻고, infrastructure는 `SystemClockAdapter`로 구현한다.

## 6. Persistence / Schema

Layer: App / Infrastructure / Contract

- Flyway migration은 `pabal-app` resource에 둔다.
- JPA는 `ddl-auto: validate`로 schema 검증만 한다.
- `open-in-view: false`를 사용한다.
- `State`/`Persisted*`는 contract, JPA Entity는 infrastructure, domain entity는 domain에 둔다.
- schema와 DB 제약은 [Pabal 데이터베이스 스키마와 제약](database-schema-and-constraints.md)에서 본다.

## 7. Observability

Layer: App / Common / Infrastructure

- `spring-boot-starter-opentelemetry`와 local OTel collector가 준비되어 있다.
- `GlobalExceptionHandler`는 `ApiError.traceId`와 logging MDC를 연결한다.
- `/actuator/health`는 인증 없이 접근 가능하다.
- 운영 exporter와 alerting 정책은 아직 별도 결정이 필요하다.

상세 내용은 [Pabal Observability와 운영 설정](observability-and-operations.md)에서 본다.

## 운영상 주의점

- 신규 repository method는 tenant 조건을 빠뜨리면 안 된다.
- 신규 realtime destination은 CONNECT 인증과 SUBSCRIBE 인가를 분리해서 설계해야 한다.
- 신규 domain event는 after-commit 필요 여부를 명시해야 한다.
- 신규 API error는 `ErrorCode`와 `GlobalExceptionHandler`의 정규화 규칙을 확인해야 한다.
- schema 변경은 Flyway, JPA Entity, persistence contract, 테스트를 함께 갱신해야 한다.
