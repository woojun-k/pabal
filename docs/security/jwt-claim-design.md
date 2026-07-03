---
tags:
  - pabal
  - security
  - jwt
---

# Pabal 보안과 JWT Claim 설계

> 상위 문서: [Pabal 상세 설계 허브](../design/design-hub.md)
> 관련 문서: [Pabal Authorization Governance와 RBAC Permission 모델](authorization-governance.md), [Pabal 인가 경계와 멀티테넌시 체크포인트](authorization-and-multitenancy.md), [Pabal STOMP 연동 가이드](../realtime/stomp-guide.md), [Websocket 설정](../realtime/websocket-configuration.md)

## 개요

Layer: Security
Status: Implemented

Pabal은 JWT를 `PabalPrincipal(userId, tenantId, subject)`로 정규화한다. 이 principal이 HTTP command/query와 STOMP 인증/인가의 기준이며, role/permission authority는 application `PermissionPort` 구현에서 RBAC 판단의 입력으로만 사용한다. JWT converter는 authority normalization만 담당하고, role-permission matrix와 persisted user-role assignment는 [Authorization Governance 문서](authorization-governance.md)와 `rbac_*` 테이블을 기준으로 관리한다.

`PabalPrincipal`은 Spring Messaging 타입을 노출하지 않는다. STOMP user destination에 필요한 `DestinationUserNameProvider` 구현은 messenger infrastructure의 adapter 타입이 담당한다.

## 설정 속성

Code: `JwtSecurityProperties`

Prefix: `pabal.security.jwt`

| Property | 기본/현재 값 | 의미 |
| --- | --- | --- |
| `issuer-uri` | env 또는 local-dev | issuer 검증 |
| `audience` | `pabal-api` | audience 검증 |
| `user-id-claim` | `uid` | `PabalPrincipal.userId` source |
| `tenant-id-claim` | `tenant_id` | `PabalPrincipal.tenantId` source |
| `principal-claim` | `sub` | `PabalPrincipal.subject` source |
| `clock-skew` | `30s` | token validation skew |
| `access-token-min-ttl` | `60m` | 내부 issuer/local access token의 최소 TTL |
| `access-token-max-ttl` | `90m` | 내부 issuer/local access token의 최대 TTL |
| `refresh-token-ttl` | `7d` | 내부 issuer/local refresh token TTL |
| `refresh-token.reuse-grace-period` | `3s` | used refresh token replay 허용 시간 |
| `refresh-token.request-idempotency-ttl` | `30s` | `X-Request-ID` refresh 응답 Redis cache TTL |
| `local-secret` | env 또는 process 내 random | local/test HS256 token secret |

## principal mapping

Code:

- `PabalJwtAuthenticationConverter`
- `PabalPrincipal`
- `PabalJwtAuthenticationToken`

흐름:

```text
Jwt
→ PabalJwtAuthenticationConverter
→ PabalPrincipal(userId, tenantId, subject)
→ PabalJwtAuthenticationToken
```

필수 claim 중 하나라도 없으면 JWT conversion은 실패한다.

## authority mapping

Code:

- `PabalJwtAuthenticationConverter`
- `AuthorityNormalizer`

기본 Spring scope mapping에 더해 다음 claim을 authority로 병합한다.

| Claim | 입력 예 | Authority |
| --- | --- | --- |
| `scope`, `scp` | `messenger:channel:create` | `SCOPE_messenger:channel:create` |
| `permissions` | `["messenger:channel:create"]` | `messenger:channel:create` |
| `roles` | `["tenant_admin"]` | `ROLE_TENANT_ADMIN` |
| `roles` | `["tenant:{tenantId}:admin"]` | `ROLE_TENANT_{NORMALIZED_TENANT_ID}_ADMIN` |
| `roles` | `["workspace:{workspaceId}:owner"]` | `ROLE_WORKSPACE_{NORMALIZED_WORKSPACE_ID}_OWNER` |
| `realm_access.roles` | Keycloak realm role | `ROLE_*` |
| `resource_access.*.roles` | Keycloak client role | `ROLE_*` |

role은 대문자와 underscore로 정규화하고 `ROLE_` prefix가 없으면 추가한다. `:`와 `-`도 `_`로 바꿔 scoped role을 안정적으로 비교한다. permission 값은 bounded context application의 `FineGrainedPermission.value()`와 맞춰야 한다.

JWT claim 관리 원칙:

- JWT는 tenant/user identity와 authority input을 전달한다.
- JWT는 permission source of truth가 아니다. 운영 권한 변경 반영은 `RbacPermissionStore`의 DB/cache 조회 경로가 담당한다.
- JWT converter는 role-permission mapping을 알지 않는다.
- role은 coarse-grained RBAC 입력이고, use case에서는 fine-grained permission으로 판정한다.
- 가능하면 `tenant:{tenantId}:{permission}`, `workspace:{workspaceId}:{permission}`, `room:{chatRoomId}:{permission}` 같은 scoped authority를 사용한다.
- workspace owner/admin 같은 업무 role source는 JWT만 신뢰하지 않고 `WorkspaceContract`로도 조회한다.

## access token / refresh token lifecycle

Layer: Security / App

Status: Implemented for internal/local issuer path; external IdP refresh remains provider-owned

Pabal의 권장 운영 모델은 짧은 access token, opaque refresh token, DB/Redis-backed RBAC permission check 조합이다.

```text
Access Token
  - JWT
  - 발급 시 TTL을 60~90분 사이에서 무작위로 할당
  - tenant/user identity와 최소 authority input만 포함

Refresh Token
  - opaque random token
  - DB에는 SHA-256 hash만 저장
  - 기본 TTL 7일
  - refresh 시 rotate: 기존 token에 used_at/revoked_at/replaced_by_token_id 기록, 새 token 저장
  - used token replay가 3초 grace period 안이면 Redis cache의 기존 token pair 반환
  - X-Request-ID가 같으면 30초 TTL 동안 Redis cache에서 동일 응답 반환

Authorization Check
  - JWT identity 확인
  - RbacPermissionStore가 DB/Redis에서 current permission 조회
```

Code:

- `RefreshTokenService`
- `RefreshTokenStore`
- `JdbcRefreshTokenStore`
- `RedisRefreshTokenReplayCache`
- `RefreshTokenController`
- Flyway `V9__security_refresh_tokens.sql`

HTTP endpoint:

```text
POST /api/v1/auth/tokens/refresh
POST /api/v1/auth/tokens/revoke
```

`/api/v1/auth/tokens/refresh`는 refresh token 자체가 credential이므로 bearer access token 없이 접근 가능하다. 유효한 refresh token이면 새 access token과 새 refresh token을 반환하고, 기존 refresh token은 `used_at`과 함께 revoke한다. 서버는 성공 응답을 Redis에 짧게 저장한다. 같은 `X-Request-ID`와 refresh token hash 조합이 다시 들어오면 DB 조회 전에 동일 응답을 반환하고, 이미 사용된 refresh token이 grace period 안에 다시 들어오면 `used_at` 기준으로 Redis replay cache의 기존 token pair를 반환한다. DB `used_at` 기준 grace period는 탈취 replay가 정상 요청처럼 처리되는 시간을 줄이기 위해 3초로 짧게 유지하고, Redis `X-Request-ID` idempotency TTL은 네트워크 재시도 완충을 위해 30초로 둔다. grace period를 지난 재사용이 감지되면 replacement chain을 best-effort revoke해 탈취된 token family의 추가 사용을 차단한다. 서버는 Pabal이 발급하는 Base64 URL opaque token 형식과 맞지 않는 refresh token을 DB 조회 전에 거부한다.

운영 profile에서는 refresh/revoke endpoint에 rate limiting을 적용해야 한다. 현재 repository 구현은 token format 검증과 DB hash 조회만 포함하고, request rate limiting은 gateway/filter 계층 후속 작업으로 남긴다.

권한 변경 반영 규칙:

- access token claim 변경은 token 만료 또는 refresh 이후 반영된다.
- persisted RBAC permission 변경은 `RbacPermissionStore` 조회 경로에서 반영된다.
- role/assignment 변경 use case는 `RefreshTokenService.revokeUserTokens(tenantId, userId)`와 `RbacPermissionStore.evictPermissionValues(tenantId, userId)`를 호출해야 한다.

외부 IdP를 사용하는 운영 profile에서는 refresh token 발급/회전이 IdP 책임일 수 있다. 이 경우 Pabal은 resource server로 access token을 검증하고, application authorization은 동일하게 DB/Redis-backed RBAC check를 사용한다.

## HTTP security

Code: `SecurityConfig`

허용:

- `/actuator/health`
- websocket endpoint path와 하위 path
- `/api/v1/tenant-registrations/**`
- `/dev/**` only when `local` or `test` profile is active

그 외:

- authenticated
- stateless session
- OAuth2 resource server JWT
- basic/form/csrf disabled

## local/test token

Code:

- `LocalJwtConfig`
- `LocalDevTokenController`

`GET /dev/token?userId={uuid}&tenantId={uuid}`는 local/test profile에서 access token만 발급한다. 이 개발 편의 endpoint는 refresh token row를 DB에 저장하지 않으며, claim은 설정된 claim name에 맞춰 들어간다.

`local-secret` 값이 비어 있거나 기존 문서 예시 placeholder이면 `LocalJwtConfig`가 process 내 random key를 생성해 `JwtEncoder`와 `JwtDecoder`가 공유한다. `scripts/run-local.sh`, `scripts/run-test.sh`는 가능한 경우 `openssl rand -hex 32`로 같은 값을 환경 변수에 먼저 주입한다. 고정 local/test token이 필요한 경우에만 고유 secret을 `.env.local` 또는 `.env.test`에 저장한다.

권한 테스트가 필요하면 query parameter를 추가한다.

```text
GET /dev/token?userId={uuid}&tenantId={uuid}&role=workspace_admin
GET /dev/token?userId={uuid}&tenantId={uuid}&scope=messenger:channel:create
GET /dev/token?userId={uuid}&tenantId={uuid}&permission=messenger:channel:create
```

`role`, `scope`, `permission`은 반복 parameter로 여러 개 전달할 수 있다. 응답에는 `accessToken`, `accessTokenExpiresAt`이 포함된다.

## current authentication provider

Code:

- `CurrentAuthenticationProvider`
- `SecurityContextCurrentAuthenticationProvider`

`SecurityContextHolder` 접근은 security module에 둔다. Messenger infrastructure는 이 provider를 통해 현재 `PabalPrincipal`과 authority set을 읽고, application에는 `PermissionPort` 결과만 전달한다.

## 운영 profile decoder

Code: `IssuerJwtDecoderConfig`

- issuer location으로 `JwtDecoder`를 만든다.
- issuer, audience, required claims를 검증한다.

## WebSocket principal adapter

Code:

- `StompAuthenticationToken`
- `RealtimePrincipal`

`PabalPrincipal`은 security module의 protocol-neutral principal이다. STOMP CONNECT 인증 후 messenger infrastructure가 `PabalJwtAuthenticationToken`을 `StompAuthenticationToken`으로 감싸고, 이 adapter가 `DestinationUserNameProvider`를 구현한다.

```text
RealtimePrincipal.destinationUserName(tenantId, userId)
= {tenantId}:{userId}
```

`StompChatRealtimeAdapter.publishSubscriptionRevocation`은 이 destination user name을 사용해 `/user/queue/chat.control`로 전송한다. 이 경계를 통해 `pabal-security`는 `spring-messaging`에 의존하지 않는다.

## 보안 설계 원칙

- 클라이언트가 제공한 userId/tenantId를 신뢰하지 않는다.
- HTTP command/query는 authentication principal에서 tenant/user를 꺼낸다.
- `pabal-security`는 HTTP/JWT principal 정규화를 담당하고, `pabal-authorization`은 cross-cutting RBAC permission lookup을 담당한다. STOMP 전용 user destination adapter는 어느 쪽도 소유하지 않는다.
- STOMP typing payload의 tenantId는 principal tenant와 일치해야 한다.
- room/topic subscribe는 tenant 일치와 active membership을 모두 확인한다.
- role은 coarse-grained RBAC 입력이고, use case에서는 fine-grained permission으로 판정한다.
