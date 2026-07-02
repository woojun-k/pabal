---
tags:
  - pabal
  - architecture
  - backlog
  - technical-debt
---

# Pabal 기술 부채와 보강 목록

> 상위 문서: [Pabal 상세 설계 허브](../design/design-hub.md)
> 관련 문서: [Pabal 아키텍처 개요](overview.md), [Pabal 멀티모듈 전환 전략](multi-module-transition.md), [Pabal MSA 전환 준비 체크리스트](msa-readiness-checklist.md), [Pabal 테스트 전략](../testing/testing-strategy.md), [Pabal Authorization Governance와 RBAC Permission 모델](../security/authorization-governance.md), [Pabal 인가 경계와 멀티테넌시 체크포인트](../security/authorization-and-multitenancy.md), [Pabal 데이터베이스 스키마와 제약](database-schema-and-constraints.md), [Pabal 이벤트 발행과 트랜잭션 경계](event-and-transaction-boundary.md)

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
| P1 | authorization governance 후속 정책 | Proposed | [Pabal Authorization Governance와 RBAC Permission 모델](../security/authorization-governance.md) |
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
- `RealtimePrincipal`은 user destination name 생성에 사용된다.
- 현재 STOMP 인증은 `StompConnectAuthenticationInterceptor` + `WebSocketAuthenticationManagerConfig` + `PabalJwtAuthenticationConverter` + `StompAuthenticationToken` 흐름이다.

선택지:

- `RealtimeAccessTokenAuthenticator`가 사용되지 않는 타입이면 삭제한다.
- Realtime Gateway 분리 준비 타입이면 `Status: Planned`로 문서화하고 실제 사용 계획을 남긴다.

검증 방법:

- `rg "RealtimeAccessTokenAuthenticator"`로 참조 여부 확인.
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

## 5. authorization governance 후속 정책

Status: Proposed
Layer: Application / Security / Infrastructure

현재 확인된 상태:

- workspace와 workspace membership의 source of truth는 `workspace`, `workspace_member`와 `WorkspaceContract`로 구현되어 있다.
- `RoomParticipantPolicy`는 channel participant validation 시 `WorkspaceContract`로 active workspace member를 batch 조회한다.
- `ChatRoomAuthorizationService`는 channel create와 channel deletion을 fine-grained `MessengerPermission`으로 판정한다.
- `PermissionPort`는 application boundary에 있고, `RbacPermissionAdapter`가 JWT authority, DB RBAC permission, workspace-owned role을 permission으로 변환한다.
- `pabal-authorization`의 `RbacPermissionStore`/`JdbcRbacPermissionStore`가 `rbac_*` 테이블 기반 tenant/user별 persisted permission 조회와 optional Redis read-through cache를 제공한다.
- `security_refresh_token`과 `RefreshTokenService`가 짧은 access token + opaque refresh token rotation을 제공한다. `JdbcRefreshTokenStore`의 JDBC 의존은 refresh token 보안 인프라이므로 `pabal-security`에 남긴다.
- `pabal-infra-redis`가 Redis dependency boundary를 제공해 authorization cache와 향후 module별 cache layer가 Redis starter를 직접 중복 선언하지 않게 한다.
- `TenantPermission`, `UserPermission`, `WorkspacePermission`, `MessengerPermission` catalog가 `rbac_permission` seed와 연결되어 있다.
- tenant owner/admin/pabal admin은 모든 Messenger permission, workspace owner/admin은 channel create/invite와 any deletion, channel owner는 own deletion을 가진다.
- workspace owner/admin은 `workspace_member.role` 조회와 JWT role authority 양쪽에서 판정한다.
- 세부 설계와 module별 role/permission ownership은 [Pabal Authorization Governance와 RBAC Permission 모델](../security/authorization-governance.md)에 정리되어 있다.

결정해야 할 정책:

- room-scoped permission과 workspace-scoped permission을 운영 IAM에서 어떤 claim 형태로 발급할 것인가?
- tenant owner/admin bootstrap과 role administration API를 어떻게 제공할 것인가?
- workspace/user management permission enforcement를 어떤 use case부터 연결할 것인가?
- role/assignment 관리 API에서 RBAC permission cache evict와 refresh token revoke를 어떻게 호출할 것인가?
- authorization 관리 API가 커질 때 `pabal-authorization` 내부의 API/application/infrastructure 세분화를 언제 적용할 것인가?
- authorization decision audit log를 어떤 shape로 남길 것인가?
- `/api/v1/auth/tokens/refresh`, `/api/v1/auth/tokens/revoke`에 운영 rate limiting을 어떤 filter/gateway 계층에서 적용할 것인가?
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
- [ ] 관련 `docs/` 문서를 갱신했는가?

## 10. TenantRegistration domain verification/activation 분리와 persistence/application/api 반영 (해소됨)

Status: Implemented
Layer: Domain / Contract / Infrastructure / Application / API

현재 확인된 상태:

- `pabal-tenant-domain`의 `TenantRegistration`, `TenantRegistrationStatus`, `TenantRegistrationSnapshot`은 단일 `expiresAt`을 `verificationExpiresAt`/`activationExpiresAt`으로 분리하고, status를 `PENDING_VERIFICATION`, `DOMAIN_VERIFIED`, `REVERIFICATION_REQUIRED`, `ACTIVATED`, `EXPIRED` 5개로 확장했다. `activate()`는 `activationExpiresAt`을 넘긴 activation을 `TenantRegistrationExpiredException`으로 거부하고, `requireReverification`/`reverify`로 lapsed activation window를 관측 가능한 상태로 전환/복구할 수 있다. 읽기 전용 guard `validateReverificationAllowed()`가 `ReverifyTenantRegistrationCommandHandler`의 DNS 조회 이전 상태 검증에 쓰인다.
- `pabal-tenant-contract`의 `TenantRegistrationState`도 같은 두 timestamp 필드를 반영하도록 갱신되었다.
- `pabal-tenant-infrastructure`의 persistence + scheduler 계층은 domain/contract를 따라잡았다: `V10__tenant_registration_split_expiry_and_reverification_status.sql`이 `expires_at`을 `verification_expires_at`(NOT NULL)/`activation_expires_at`(nullable)로 분리하고 5-status CHECK와 status별 timestamp CHECK를 갱신했으며, `TenantRegistrationEntity`는 새 `TenantRegistrationState` 13-arg 생성자 순서에 맞춰 두 timestamp를 매핑한다. `TenantRegistrationRepositoryImpl.OPEN_STATUSES`는 `DOMAIN_VERIFIED`/`REVERIFICATION_REQUIRED`를 포함한 4개 open status를 사용하고, `TenantRegistrationExpirationScheduler.reverifyLapsedRegistrations()`가 lapsed `DOMAIN_VERIFIED` registration을 도메인 메서드 `requireReverification(now)`를 통해서만 `REVERIFICATION_REQUIRED`로 전이하는 sweep을 `pabal.tenant.registration.reverification-sweep-delay-ms`(기본 600000ms) 간격으로 실행한다.
- `pabal-tenant-application`의 `RequestTenantRegistrationCommandHandler`/`RenewTenantRegistrationTokenCommandHandler`(verification window)와 `VerifyTenantDomainCommandHandler`(activation window)는 더 이상 하드코딩된 `Duration` 상수를 갖지 않는다. window 값은 `pabal.tenant.registration.verification-window-ms`/`activation-window-ms`(둘 다 기본 604800000ms = 7일) 프로퍼티로 외부화되어 `@Value` 생성자 주입된다. 새 `ReverifyLapsedTenantRegistrationsCommand`/`CommandHandler<…, Integer>`가 sweep 유스케이스를 구현한다.
- `VerifyTenantDomainCommandHandler`는 verify-only handler로 재작성되어 DNS 검증 후 `markVerified`로 `DOMAIN_VERIFIED`에서 멈추고 `Tenant`를 생성하지 않는다(`TenantRepository` 의존성 자체가 없다). Activation은 새 `ActivateTenantRegistrationCommandHandler`가 `findByIdForUpdate` 락 이후 `activate(tenantId, now)`로 수행하며, `Tenant`를 정확히 1건 생성하고 registration에 연결한다. 단일 registration에 대한 명시적 재검증은 새 `ReverifyTenantRegistrationCommandHandler`(DNS 조회 이전에 `validateReverificationAllowed()`로 상태를 먼저 검증)가 `reverify(now, now+window)`로 수행한다.
- `pabal-tenant-api`는 새 endpoint `POST /api/v1/tenant-registrations/{registrationId}/activation`, `POST /api/v1/tenant-registrations/{registrationId}/reverification`을 노출하고, `TenantRegistrationResponse`/`TenantRegistrationDetailResponse`/`VerifyTenantDomainResponse`/`ActivateTenantRegistrationResponse`/`ReverifyTenantRegistrationResponse` 모두 단일 `expiresAt` 대신 `verificationExpiresAt`/`activationExpiresAt`을 노출한다. `status` 필드는 5-status 문자열을 그대로 echo한다.
- `REVERIFICATION_REQUIRED` row는 `activationExpiresAt + reverification-grace-ms <= now`가 되면 `TenantRegistrationRepository.expireLapsedReverificationRegistrations(Instant, long)`(bulk UPDATE)를 통해 `EXPIRED`로 닫힌다. 새 프로퍼티 `pabal.tenant.registration.reverification-grace-ms`(기본 604800000ms)로 grace window 길이를 제어하며, `TenantRegistrationExpirationScheduler.expirePendingRegistrations()`가 기존 pending expiry sweep과 함께 실행한다. `DOMAIN_VERIFIED` row는 이 sweep이 건드리지 않는다 — 먼저 `reverifyLapsedRegistrations()`로 `REVERIFICATION_REQUIRED`를 거쳐야 한다.
- 이 결정의 배경은 [ADR-0013](../adr/0013-split-overloaded-expiry-timestamp-into-verification-and-activation-windows.md)에 있다(follow-up 항목 완료 반영됨).

필요했던 후속 작업(전부 완료):

- [x] `V10__*.sql`로 `expires_at` → `verification_expires_at`/`activation_expires_at` 컬럼 분리와 5-status CHECK constraint 갱신
- [x] `TenantRegistrationEntity` 필드/매핑을 새 `TenantRegistrationState` signature에 맞춰 갱신
- [x] `TenantRegistrationExpirationScheduler`에 lapsed `DOMAIN_VERIFIED` → `REVERIFICATION_REQUIRED` sweep 추가(`reverifyLapsedRegistrations()`, `requireReverification(now)`를 통해서만 전이)
- [x] verification/activation window `Duration` 상수를 `pabal.tenant.registration.verification-window-ms`/`activation-window-ms` 프로퍼티로 외부화
- [x] two-phase `VerifyTenantDomainCommandHandler`(DNS 검증 → `DOMAIN_VERIFIED`)와 별도 activation(`ActivateTenantRegistrationCommandHandler`)/reverification(`ReverifyTenantRegistrationCommandHandler`) command handler 도입
- [x] API 응답에 새 status와 두 timestamp 반영, 새 activation/reverification endpoint 노출
- [x] `REVERIFICATION_REQUIRED`의 terminal expiry(→ `EXPIRED`) grace-window 정책 결정 및 구현(`pabal.tenant.registration.reverification-grace-ms`)
- [x] `docs/use-cases/command-query-catalog.md`, `docs/use-cases/http-api-and-error-mapping.md`, `docs/architecture/database-schema-and-constraints.md`의 `Status: Partial` 표기를 `Implemented`로 갱신

이 항목은 domain/contract/infrastructure/application/api 전 계층이 정합해 `Status: Implemented`로 닫혔다.
