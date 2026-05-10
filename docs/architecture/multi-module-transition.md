---
tags:
  - pabal
  - architecture
  - multimodule
  - transition
---

# Pabal 멀티모듈 전환 전략

> 상위 문서: [Pabal 아키텍처 개요](overview.md)
> 관련 문서: [Pabal 패키지 구조와 레이어](package-structure-and-layers.md), [Pabal MSA 전환 준비 체크리스트](msa-readiness-checklist.md), [Pabal 기술 부채와 보강 목록](technical-debt.md), [Pabal Persistence 경계와 데이터 변환](persistence-boundary-and-mapping.md), [Pabal 테스트 전략](../testing/testing-strategy.md)

## 상태

Status: Implemented

현재 작업 트리 기준으로 Gradle module은 다음과 같이 분리되어 있다.

```text
pabal-app
pabal-common
pabal-security
pabal-messenger-domain
pabal-messenger-application
pabal-messenger-contract
pabal-messenger-api
pabal-messenger-infrastructure
```

현재 목표는 MSA가 아니라, 단일 배포 안에서 모듈 경계를 명확히 하는 멀티모듈 모놀리스다.

## 목표 의존 방향

```text
api → application
application → domain
application → contract
contract → domain
infrastructure → application/domain/contract
security → common
app → api/application/infrastructure/security/common
```

`common`은 모든 모듈이 사용할 수 있지만, 특정 messenger 구현을 알아서는 안 된다.

## 금지 의존

```text
domain → contract
domain → infrastructure
domain → api
application → infrastructure
api → infrastructure
contract → infrastructure
security → messenger-*
common → messenger-*
```

## 모듈별 안정화 기준

### pabal-app

Layer: App

- Spring Boot plugin을 가진 유일한 실행 모듈이다.
- application resource, Flyway migration, local/test runtime 설정을 소유한다.
- domain/application 세부 정책을 직접 구현하지 않는다.

### pabal-common

Layer: Common

- `ApiError`, `GlobalExceptionHandler`, CQRS marker, event publisher, UUID v7를 제공한다.
- 도메인 전용 개념을 넣지 않는다.

### pabal-security

Layer: Security

- JWT claim mapping과 `PabalPrincipal`을 소유한다.
- room/member authorization 정책은 messenger application/infrastructure에 남긴다.

### pabal-messenger-domain

Layer: Domain

- entity, VO, policy, domain event, domain exception만 둔다.
- repository port는 application에 둔다.
- `State`, `Persisted*`, JPA Entity를 import하지 않는다.

### pabal-messenger-contract

Layer: Contract

- persistence state/wrapper/mapper와 realtime payload/envelope을 둔다.
- 외부 시스템 계약 후보를 안정화하는 위치다.
- 비즈니스 결정을 하지 않는다.

### pabal-messenger-application

Layer: Application

- command/query handler, support/service, outbound port, event listener를 둔다.
- transaction boundary와 orchestration을 담당한다.
- infrastructure 구현체를 참조하지 않는다.

### pabal-messenger-api

Layer: API

- HTTP/STOMP controller와 mapper를 둔다.
- `PabalPrincipal`에서 tenant/user를 추출해 command/query에 반영한다.
- application handler에 위임한다.

### pabal-messenger-infrastructure

Layer: Infrastructure

- JPA adapter, JPA Entity, Spring Data repository, STOMP adapter, WebSocket authorization, clock adapter를 둔다.
- application port를 구현한다.
- 유스케이스 정책을 새로 만들지 않는다.

## 단계별 전환 체크리스트

Status: Partial

- [x] Gradle module 분리
- [x] domain/application/api/contract/infrastructure source 이동
- [x] repository port를 application outbound port로 정리
- [x] persistence contract와 JPA Entity 분리
- [x] STOMP adapter를 infrastructure로 격리
- [x] message send use case interface와 transaction-owning adapter 분리
- [ ] 모듈 의존 규칙 자동 검증 추가
- [ ] 전체 `./gradlew test` 기준으로 module boundary regression 확인
- [x] `message.content` 길이 정책 불일치 정리
- [x] channel create/deletion RBAC permission port 도입
- [ ] workspace/channel role source of truth 외부 모델 연동
- [ ] unused realtime security 타입 정리 여부 결정
- [ ] WebSocket 보안 테스트 보강
- [ ] realtime contract versioning 도입 여부 결정
- [ ] outbox/event delivery 전환 시점 결정

## MSA 전환과의 관계

멀티모듈 전환은 MSA 전환의 선행 조건일 뿐이다. 현재 상태에서 바로 MSA로 분리하면 schema ownership, transaction boundary, event delivery, authorization policy가 모두 불안정하다.

MSA 전환 판단은 [Pabal MSA 전환 준비 체크리스트](msa-readiness-checklist.md)를 따른다.
