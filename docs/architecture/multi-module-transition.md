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
pabal-web
pabal-security
pabal-authorization
pabal-infra-redis
pabal-persistence-support
pabal-tenant-domain
pabal-tenant-application
pabal-tenant-contract
pabal-tenant-api
pabal-tenant-infrastructure
pabal-workspace-domain
pabal-workspace-application
pabal-workspace-contract
pabal-workspace-api
pabal-workspace-infrastructure
pabal-user-domain
pabal-user-application
pabal-user-contract
pabal-user-api
pabal-user-infrastructure
pabal-messenger-domain
pabal-messenger-application
pabal-messenger-contract
pabal-messenger-api
pabal-messenger-infrastructure
```

현재 목표는 MSA가 아니라, 단일 배포 안에서 모듈 경계를 명확히 하는 멀티모듈 모놀리스다.

## 목표 의존 방향

```text
{bounded-context}-api → {bounded-context}-application
{bounded-context}-application → {bounded-context}-domain
{bounded-context}-application → {bounded-context}-contract
{bounded-context}-contract → {bounded-context}-domain
{bounded-context}-infrastructure → {bounded-context}-application/{bounded-context}-domain/{bounded-context}-contract
{bounded-context}-infrastructure → pabal-persistence-support
pabal-persistence-support → pabal-common
security → authorization/common
authorization → infra-redis/common
web → common
app → *-api/*-application/*-infrastructure/security/authorization/infra-redis/common/web
```

`common`은 모든 모듈이 사용할 수 있지만, 특정 tenant/workspace/user/messenger 구현을 알아서는 안 된다. `TenantContract`, `WorkspaceContract`, `UserContract`처럼 bounded context 간에 필요한 최소 조회 contract만 둘 수 있다.

## 금지 의존

```text
{bounded-context}-domain → {bounded-context}-contract
{bounded-context}-domain → {bounded-context}-infrastructure
{bounded-context}-domain → {bounded-context}-api
{bounded-context}-application → {bounded-context}-infrastructure
{bounded-context}-api → {bounded-context}-infrastructure
{bounded-context}-contract → {bounded-context}-infrastructure
{bounded-context}-domain/application/api/contract → pabal-persistence-support
security → tenant-* 또는 workspace-* 또는 user-* 또는 messenger-*
authorization → tenant-* 또는 workspace-* 또는 user-* 또는 messenger-*
web → tenant-* 또는 workspace-* 또는 user-* 또는 messenger-*
common → tenant-* 또는 workspace-* 또는 user-* 또는 messenger-*
```

## 모듈별 안정화 기준

### pabal-app

Layer: App

- Spring Boot plugin을 가진 유일한 실행 모듈이다.
- application resource, Flyway migration, local/test runtime 설정을 소유한다.
- domain/application 세부 정책을 직접 구현하지 않는다.

### pabal-common

Layer: Common

- CQRS marker, event publisher abstraction, context contract, UUID v7를 제공한다.
- 도메인 전용 개념을 넣지 않는다.
- JPA/Hibernate persistence support를 넣지 않는다. 해당 책임은 `pabal-persistence-support`가 가진다.

### pabal-web

Layer: Web Support

- `ApiError`, `GlobalExceptionHandler`, `SpringDomainEventPublisher` 같은 Spring/Web 기반 공통 adapter를 제공한다.
- 특정 bounded context의 controller, request/response DTO, security policy는 소유하지 않는다.

### pabal-security

Layer: Security

- JWT claim mapping과 `PabalPrincipal`을 소유한다.
- `CurrentAuthenticationProvider`, refresh token lifecycle, HTTP security wiring을 소유한다.
- RBAC permission store/cache와 authority matching은 `pabal-authorization`에 둔다.
- STOMP 전용 `DestinationUserNameProvider`를 소유하지 않는다.
- room/member authorization 정책은 messenger application/infrastructure에 남긴다.

### pabal-authorization

Layer: Authorization

- `AuthorityNormalizer`, `PermissionAuthorityMatcher`, `RbacPermissionStore`, `JdbcRbacPermissionStore`를 소유한다.
- JWT role/permission authority와 DB-backed RBAC permission을 application `PermissionPort` adapter가 사용할 수 있는 형태로 제공한다.
- tenant/workspace/user/messenger repository나 JPA Entity를 직접 참조하지 않는다.

### pabal-infra-redis

Layer: Shared Infrastructure

- Redis starter dependency boundary를 제공한다.
- authorization cache와 향후 bounded context별 cache/pub-sub adapter가 Redis dependency를 중복 선언하지 않게 한다.
- business cache key 정책이나 authorization rule을 소유하지 않는다.

### pabal-persistence-support

Layer: Shared Infrastructure Support

- JPA `@MappedSuperclass` base entity와 Hibernate UUID v7 generator를 제공한다.
- `pabal-common`의 순수 `UuidV7` utility를 재사용할 수 있다.
- JPA Entity가 있는 infrastructure module만 의존한다.
- domain/application/API/contract 모듈의 의존 대상이 아니다.

### pabal-tenant-domain

Layer: Domain

- tenant aggregate, name/status VO, tenant domain exception만 둔다.
- repository port는 application에 둔다.
- `State`, `Persisted*`, JPA Entity를 import하지 않는다.

### pabal-tenant-contract

Layer: Contract

- tenant persistence state/wrapper/mapper를 둔다.
- 비즈니스 결정을 하지 않는다.

### pabal-tenant-application

Layer: Application

- tenant command/query handler, repository port, `TenantContractService`를 둔다.
- user/workspace가 tenant infrastructure를 직접 참조하지 않도록 common `TenantContract`를 구현한다.
- infrastructure 구현체를 참조하지 않는다.

### pabal-tenant-api

Layer: API

- tenant HTTP controller와 mapper를 둔다.
- application handler에 위임한다.

### pabal-tenant-infrastructure

Layer: Infrastructure

- `pabal_tenant` JPA adapter, JPA Entity, Spring Data repository, clock adapter를 둔다.
- application port를 구현한다.
- 유스케이스 정책을 새로 만들지 않는다.

### pabal-workspace-domain

Layer: Domain

- workspace aggregate와 workspace member aggregate, role/status invariant만 둔다.
- repository port는 application에 둔다.
- `State`, `Persisted*`, JPA Entity를 import하지 않는다.

### pabal-workspace-contract

Layer: Contract

- workspace/workspace member persistence state/wrapper/mapper를 둔다.
- 비즈니스 결정을 하지 않는다.

### pabal-workspace-application

Layer: Application

- workspace command/query handler, workspace/member repository port, `WorkspaceContractService`를 둔다.
- workspace 생성 시 `TenantContract`와 `UserContract`로 active tenant와 owner user를 검증한다.
- messenger가 workspace JPA를 직접 참조하지 않도록 common `WorkspaceContract`를 구현한다.

### pabal-workspace-api

Layer: API

- workspace HTTP controller와 mapper를 둔다.
- `PabalPrincipal`에서 tenant/user를 추출해 command/query에 반영한다.
- application handler에 위임한다.

### pabal-workspace-infrastructure

Layer: Infrastructure

- `workspace`, `workspace_member` JPA adapter, JPA Entity, Spring Data repository, clock adapter를 둔다.
- application port를 구현한다.
- 유스케이스 정책을 새로 만들지 않는다.

### pabal-user-domain

Layer: Domain

- user aggregate, name/status VO, user domain exception만 둔다.
- repository port는 application에 둔다.
- `State`, `Persisted*`, JPA Entity를 import하지 않는다.

### pabal-user-contract

Layer: Contract

- user persistence state/wrapper/mapper를 둔다.
- 비즈니스 결정을 하지 않는다.

### pabal-user-application

Layer: Application

- user command/query handler, repository port, `UserContractService`를 둔다.
- user 생성 시 `TenantContract`로 active tenant를 검증한다.
- messenger가 user repository를 직접 참조하지 않도록 common `UserContract`를 구현한다.
- infrastructure 구현체를 참조하지 않는다.

### pabal-user-api

Layer: API

- user HTTP controller와 mapper를 둔다.
- `PabalPrincipal`에서 tenant/user를 추출해 command/query에 반영한다.
- application handler에 위임한다.

### pabal-user-infrastructure

Layer: Infrastructure

- `tenant_user` JPA adapter, JPA Entity, Spring Data repository, clock adapter를 둔다.
- application port를 구현한다.
- 유스케이스 정책을 새로 만들지 않는다.

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
- [x] user bounded context module 추가
- [x] tenant bounded context module 추가
- [x] workspace bounded context module 추가
- [x] `pabal-security`에서 Spring Messaging 의존 제거
- [ ] 모듈 의존 규칙 자동 검증 추가
- [ ] 전체 `./gradlew test` 기준으로 module boundary regression 확인
- [x] `message.content` 길이 정책 불일치 정리
- [x] channel create/deletion RBAC permission port 도입
- [x] workspace membership source of truth 모듈화
- [x] workspace/channel role permission을 workspace membership role과 정합화
- [x] authorization/RBAC 조회를 `pabal-authorization`으로 분리
- [x] Redis dependency boundary를 `pabal-infra-redis`로 분리
- [x] JPA/Hibernate persistence support를 `pabal-common`에서 `pabal-persistence-support`로 분리
- [ ] unused realtime security 타입 정리 여부 결정
- [ ] WebSocket 보안 테스트 보강
- [ ] realtime contract versioning 도입 여부 결정
- [ ] outbox/event delivery 전환 시점 결정

## MSA 전환과의 관계

멀티모듈 전환은 MSA 전환의 선행 조건일 뿐이다. 현재 상태에서 바로 MSA로 분리하면 schema ownership, transaction boundary, event delivery, authorization policy가 모두 불안정하다.

MSA 전환 판단은 [Pabal MSA 전환 준비 체크리스트](msa-readiness-checklist.md)를 따른다.
