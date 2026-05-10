---
tags:
  - pabal
  - architecture
  - common
  - module
---

# Pabal 공통 모듈 설계

> 상위 문서: [Pabal Wiki Home](../README.md)
> 관련 문서: [Pabal 패키지 구조와 레이어](package-structure-and-layers.md), [Pabal 크로스커팅 관심사](cross-cutting-concerns.md), [Pabal 이벤트 발행과 트랜잭션 경계](event-and-transaction-boundary.md), [Pabal Observability와 운영 설정](observability-and-operations.md), [Pabal 에러 코드와 예외 매핑표](../use-cases/error-code-exception-mapping.md)

## 개요

Layer: Common
Status: Implemented

`pabal-common`은 여러 모듈이 공유하는 기술적/계약적 primitive를 둔다. Messenger 비즈니스 규칙, HTTP endpoint 정책, JPA adapter 구현, JWT 인증 정책은 소유하지 않는다.

공통 모듈의 기준은 다음이다.

```text
공통으로 재사용되는 최소 primitive만 둔다.
도메인별 의미가 있으면 해당 도메인 모듈에 둔다.
기술 구현 세부가 크면 infrastructure 또는 security로 분리한다.
```

## 패키지 지도

| Package | Layer | 주요 타입 | 책임 |
| --- | --- | --- | --- |
| `common.api` | Common / API Support | `ApiError`, `ApiErrorDetail`, `GlobalExceptionHandler` | 공통 오류 응답 shape와 exception mapping |
| `common.exception` | Common | `GlobalException`, `InvalidInputException` | 공통 예외 base |
| `common.exception.code` | Common | `ErrorCode`, `CommonErrorCode` | public error code/message/status 계약 |
| `common.cqrs` | Common | `Command`, `Query`, `CommandHandler`, `QueryHandler` | command/query marker와 handler contract |
| `common.event` | Common | `DomainEvent`, `DomainEventPublisher`, `SpringDomainEventPublisher` | in-process domain event 발행 abstraction |
| `common.persistence.entity.base` | Common / Infrastructure Support | `BaseEntity`, `UpdatableEntity`, `DeletableEntity` | JPA entity 공통 timestamp/delete field |
| `common.persistence.jpa` | Common / Infrastructure Support | `UuidV7Generated`, `UuidV7IdGenerator` | UUID v7 Hibernate generator |
| `common.util` | Common | `UuidV7` | UUID v7 생성/검증 utility |
| `common.contract` | Common / Contract | `UserContract`, `UserInfo` | 사용자 정보 조회를 위한 공통 contract 후보 |

## 의존 방향

Layer: Common

허용되는 방향:

```text
pabal-messenger-* → pabal-common
pabal-security → pabal-common
pabal-app → pabal-common
```

피해야 하는 방향:

```text
pabal-common → pabal-messenger-*
pabal-common → pabal-security
pabal-common → pabal-app
```

공통 모듈은 Spring Framework, validation, webmvc, security core, OpenTelemetry API 같은 범용 dependency를 사용할 수 있지만, 프로젝트 내부의 특정 도메인 모듈에는 의존하지 않아야 한다.

## API 오류 모델

Layer: Common / API Support

`ApiError`는 public 오류 응답의 표준 shape다.

```text
timestamp
status
code
message
path
traceId
details
```

`GlobalExceptionHandler`는 다음을 정규화한다.

- `GlobalException` → 각 `ErrorCode` 기준 응답
- validation 예외 → `CommonErrorCode.INVALID_INPUT`
- `AccessDeniedException` → `CommonErrorCode.FORBIDDEN`
- optimistic locking / data integrity violation → conflict 응답
- 기타 예외 → internal server error

trace id는 `Span.current().getSpanContext()`가 유효하면 OpenTelemetry trace id를 사용하고, 없으면 MDC의 `traceId`를 사용한다. 관련 운영 관점은 [Pabal Observability와 운영 설정](observability-and-operations.md)에서 본다.

## CQRS marker

Layer: Common / Application Support

`Command`, `Query`, `CommandHandler`, `QueryHandler`는 application layer의 use case contract를 가볍게 표준화한다.

예시 흐름:

```text
API Mapper
→ SendMessageCommand
→ CommandHandler<SendMessageCommand, SendMessageResult>
```

marker는 구조를 설명하는 역할이므로 비즈니스 규칙을 담지 않는다.

## Event publisher

Layer: Common / Application Support

`DomainEventPublisher`는 두 발행 방식을 제공한다.

- `publishNow(DomainEvent event)`
- `publishAfterCommit(DomainEvent event)`

`SpringDomainEventPublisher.publishAfterCommit`은 실제 transaction이 없으면 `IllegalStateException`을 던진다. 이벤트 발행 상세는 [Pabal 이벤트 발행과 트랜잭션 경계](event-and-transaction-boundary.md)를 기준으로 본다.

## UUID v7

Layer: Common / Infrastructure Support

- `UuidV7.random()`
- `UuidV7.monotonic()`
- `UuidV7.timestampMillis(UUID)`
- `UuidV7.timestampInstant(UUID)`
- `UuidV7.isV7(UUID)`

JPA Entity는 `@UuidV7Generated(mode = UuidV7Generated.Mode.MONOTONIC)`을 통해 UUID v7 ID를 생성한다. 현재 generator는 assigned identifier를 허용하므로 테스트나 reconstitution 경로에서 ID를 명시할 수 있다.

DB fallback인 `uuidv7()` 함수는 [Pabal 데이터베이스 스키마와 제약](database-schema-and-constraints.md)에서 다룬다.

## common에 추가해도 되는 것

- 여러 bounded context가 공유하는 오류 response shape
- 특정 도메인 의미가 없는 command/query/event marker
- 공통 ID/time/persistence support primitive
- 여러 adapter에서 반복되는 낮은 수준의 기술 helper

## common에 넣으면 안 되는 것

- Messenger 전용 domain invariant
- room/message/channel 정책
- 특정 API endpoint의 request/response DTO
- JWT issuer/audience 정책
- JPA repository adapter 구현
- 외부 broker, DB, WebSocket 세부 연결 정책

## 변경 체크리스트

- [ ] 새 타입이 특정 도메인 용어를 포함하는가? 그렇다면 domain/application/contract 쪽이 맞다.
- [ ] common이 messenger/security/app 모듈에 의존하지 않는가?
- [ ] public error code를 추가했다면 [Pabal 에러 코드와 예외 매핑표](../use-cases/error-code-exception-mapping.md)를 갱신했는가?
- [ ] event publisher 동작을 바꿨다면 [Pabal 이벤트 발행과 트랜잭션 경계](event-and-transaction-boundary.md)와 테스트를 갱신했는가?
