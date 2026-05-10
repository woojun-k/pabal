---
tags:
  - pabal
  - security
  - jwt
---

# Pabal 보안과 JWT Claim 설계

> 상위 문서: [Pabal 상세 설계 허브](../design/design-hub.md)
> 관련 문서: [Pabal 인가 경계와 멀티테넌시 체크포인트](authorization-and-multitenancy.md), [Pabal STOMP 연동 가이드](../realtime/stomp-guide.md), [Websocket 설정](../realtime/websocket-configuration.md)

## 개요

Layer: Security
Status: Implemented

Pabal은 JWT를 `PabalPrincipal(userId, tenantId, subject)`로 정규화한다. 이 principal이 HTTP command/query와 STOMP 인증/인가의 기준이며, role/permission authority는 application `PermissionPort` 구현에서 RBAC 판단에 사용한다.

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
| `local-secret` | env | local/test HS256 token secret |

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

Code: `PabalJwtAuthenticationConverter`

기본 Spring scope mapping에 더해 다음 claim을 authority로 병합한다.

| Claim | 입력 예 | Authority |
| --- | --- | --- |
| `scope`, `scp` | `messenger:channel:create` | `SCOPE_messenger:channel:create` |
| `permissions` | `["messenger:channel:create"]` | `messenger:channel:create` |
| `roles` | `["tenant_admin"]` | `ROLE_TENANT_ADMIN` |
| `realm_access.roles` | Keycloak realm role | `ROLE_*` |
| `resource_access.*.roles` | Keycloak client role | `ROLE_*` |

role은 대문자와 underscore로 정규화하고 `ROLE_` prefix가 없으면 추가한다. permission 값은 Messenger application의 `MessengerPermission.value()`와 맞춰야 한다.

## HTTP security

Code: `SecurityConfig`

허용:

- `/actuator/health`
- websocket endpoint path와 하위 path
- `/dev/*`

그 외:

- authenticated
- stateless session
- OAuth2 resource server JWT
- basic/form/csrf disabled

## local/test token

Code:

- `LocalJwtConfig`
- `LocalDevTokenController`

`GET /dev/token?userId={uuid}&tenantId={uuid}`는 local/test profile에서 access token을 발급한다. claim은 설정된 claim name에 맞춰 들어간다.

권한 테스트가 필요하면 query parameter를 추가한다.

```text
GET /dev/token?userId={uuid}&tenantId={uuid}&role=workspace_admin
GET /dev/token?userId={uuid}&tenantId={uuid}&scope=messenger:channel:create
```

`role`과 `scope`는 반복 parameter로 여러 개 전달할 수 있다.

## current authentication provider

Code:

- `CurrentAuthenticationProvider`
- `SecurityContextCurrentAuthenticationProvider`

`SecurityContextHolder` 접근은 security module에 둔다. Messenger infrastructure는 이 provider를 통해 현재 `PabalPrincipal`과 authority set을 읽고, application에는 `PermissionPort` 결과만 전달한다.

## 운영 profile decoder

Code: `IssuerJwtDecoderConfig`

- issuer location으로 `JwtDecoder`를 만든다.
- issuer, audience, required claims를 검증한다.

## WebSocket principal

`PabalPrincipal`은 `DestinationUserNameProvider`를 구현한다.

```text
PabalPrincipal.destinationUserName(tenantId, userId)
= {tenantId}:{userId}
```

`StompChatRealtimeAdapter.publishSubscriptionRevocation`은 이 destination user name을 사용해 `/user/queue/chat.control`로 전송한다.

## 보안 설계 원칙

- 클라이언트가 제공한 userId/tenantId를 신뢰하지 않는다.
- HTTP command/query는 authentication principal에서 tenant/user를 꺼낸다.
- STOMP typing payload의 tenantId는 principal tenant와 일치해야 한다.
- room/topic subscribe는 tenant 일치와 active membership을 모두 확인한다.
- role은 coarse-grained RBAC 입력이고, use case에서는 fine-grained permission으로 판정한다.
