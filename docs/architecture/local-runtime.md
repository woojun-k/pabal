---
tags:
  - pabal
  - architecture
  - runtime
  - local-dev
---

# Pabal 로컬 개발과 런타임 구성

> 상위 문서: [Pabal Wiki Home](../README.md)
> 관련 문서: [Pabal Messenger 온보딩 가이드](../onboarding/messenger-onboarding.md), [Pabal 패키지 구조와 레이어](package-structure-and-layers.md), [Pabal 데이터베이스 스키마와 제약](database-schema-and-constraints.md), [Pabal Observability와 운영 설정](observability-and-operations.md), [Pabal 테스트 전략](../testing/testing-strategy.md), [Websocket 설정](../realtime/websocket-configuration.md)

## 개요

Layer: App / Infrastructure / Security / Testing
Status: Implemented

Pabal은 `pabal-app`을 실행 모듈로 사용하는 단일 배포 멀티모듈 모놀리스다. 로컬 개발은 `local` profile, 테스트는 `test` profile을 기준으로 분리한다.

현재 런타임 구성의 기준 파일은 다음과 같다.

- `pabal-app/src/main/resources/application.yaml`
- `pabal-app/src/main/resources/application-local.yaml`
- `pabal-app/src/test/resources/application-test.yaml`
- `compose.local.yaml`
- `scripts/run-local.sh`
- `scripts/run-test.sh`

## 실행 경로

### local profile

Layer: App / Infrastructure

```text
scripts/run-local.sh
→ .env.local 로드
→ ./gradlew bootRun --args='--spring.profiles.active=local'
→ pabal-app 실행
→ Spring Boot Docker Compose가 compose.local.yaml 시작/종료 관리
```

`application-local.yaml`은 `spring.docker.compose.enabled: true`와 `lifecycle-management: start-and-stop`을 사용한다. 따라서 로컬 실행에서 PostgreSQL, Redis, OTel collector는 Spring Boot lifecycle에 묶인다.

### test profile

Layer: Testing / App

```text
scripts/run-test.sh
→ .env.test 로드
→ ./gradlew test
→ test profile resource 적용
→ Testcontainers 기반 PostgreSQL/Redis 테스트 가능
```

`pabal-app/src/test/resources/application-test.yaml`은 WebSocket endpoint를 `/ws`로 둔다. local/default runtime은 `/websocket`을 사용하므로 STOMP 테스트와 문서 예시는 profile을 구분해야 한다.

## Docker Compose 서비스

Layer: Infrastructure / Observability

| Service | Image | 기본 포트 | 역할 |
| --- | --- | --- | --- |
| `postgres` | `postgres:18.3` | `${PABAL_POSTGRES_PORT:-5432}` | Flyway/JPA runtime DB |
| `redis` | `redis:8.6-alpine` | `${PABAL_REDIS_PORT:-6379}` | Redis dependency와 health 확인 |
| `otel-collector` | `otel/opentelemetry-collector-contrib:0.151.0` | `${PABAL_OTEL_HTTP_PORT:-4318}`, `${PABAL_OTEL_GRPC_PORT:-4317}` | local trace/metric/log 수집 |

PostgreSQL과 Redis는 volume을 사용한다.

- `pabal-postgres-data`
- `pabal-redis-data`

## Profile별 주요 차이

| 항목 | default | local | test |
| --- | --- | --- | --- |
| datasource | 환경별 외부 설정 필요 | `jdbc:postgresql://localhost:${PABAL_POSTGRES_PORT:5432}/${PABAL_POSTGRES_DB:pabal}` | Testcontainers 또는 `PABAL_TEST_*` 기본값 |
| Flyway | enabled, `classpath:db/migration` | enabled | enabled |
| Hibernate DDL | `validate` | `validate` | `validate` |
| Open Session in View | `false` | `false` | 설정 파일 기준 미지정, 기본값 확인 필요 |
| JWT issuer | `${ISSUER_URI}` | `local-dev` | `PABAL_TEST_JWT_ISSUER_URI` 기본값 |
| JWT local secret | 없음 | `${PABAL_JWT_LOCAL_SECRET}` | `${PABAL_TEST_JWT_LOCAL_SECRET:...}` |
| WebSocket endpoint | `/websocket` | `/websocket` | `/ws` |
| WebSocket origin | localhost 기반 | localhost 기반 | `*` |
| STOMP broker | simple broker, relay disabled | simple broker, relay disabled | simple broker, relay disabled |

## Local JWT

Layer: Security

`LocalDevTokenController`는 `local`, `test` profile에서만 활성화된다.

```text
GET /dev/token?userId={uuid}&tenantId={uuid}
GET /dev/token?userId={uuid}&tenantId={uuid}&role=workspace_admin&scope=messenger:channel:create
```

발급된 token은 `PabalJwtAuthenticationConverter`를 거쳐 `PabalPrincipal(userId, tenantId, subject)`로 변환된다. `role`과 `scope` parameter는 JWT authority로 들어가 RBAC 테스트에 사용할 수 있다. HTTP command/query와 STOMP CONNECT는 모두 이 principal을 기준으로 tenant/user를 얻는다.

관련 설계는 [Pabal 보안과 JWT Claim 설계](../security/jwt-claim-design.md)를 기준으로 본다.

## App module의 소유 자원

Layer: App

`pabal-app`은 Spring Boot 실행과 조립을 담당하며 runtime resource를 소유한다.

- `PabalApplication`
- `application.yaml`
- `application-local.yaml`
- `db/migration/V1__postgres_extensions_and_uuidv7.sql`
- `db/migration/V2__messenger_tables.sql`
- `application-test.yaml`은 test resource에 위치

도메인, application, API, infrastructure 모듈은 실행 resource를 직접 소유하지 않는다. DB schema 문서는 [Pabal 데이터베이스 스키마와 제약](database-schema-and-constraints.md)을 기준으로 본다.

## 로컬 실행 체크리스트

- [ ] `.env.local`이 존재하는가?
- [ ] `PABAL_POSTGRES_DB`, `PABAL_POSTGRES_USER`, `PABAL_POSTGRES_PASSWORD`가 설정됐는가?
- [ ] `PABAL_JWT_LOCAL_SECRET`이 HS256에 충분한 길이로 설정됐는가?
- [ ] `/actuator/health`가 열리는가?
- [ ] `/dev/token`으로 local token을 받을 수 있는가?
- [ ] `/websocket` STOMP CONNECT에서 bearer token이 통과하는가?
- [ ] Flyway migration이 성공하고 JPA validate가 통과하는가?

## 테스트 실행 체크리스트

- [ ] `.env.test`가 존재하는가?
- [ ] `./gradlew test`가 전체 멀티모듈에서 실행되는가?
- [ ] PostgreSQL integration test는 `AbstractPostgresIntegrationTest`를 사용했는가?
- [ ] Redis integration test는 `AbstractRedisIntegrationTest`를 사용했는가?
- [ ] STOMP 테스트는 endpoint `/ws`를 기준으로 작성했는가?

## 운영 전환 시 확인할 점

Status: Planned

- STOMP relay를 사용할 경우 `pabal.websocket.relay.enabled=true`와 relay host/login/passcode 설정이 필요하다.
- local OTel collector는 debug exporter 중심이므로 운영 exporter 정책은 별도로 정해야 한다.
- local/test JWT secret 기반 token은 운영 issuer decoder와 분리해야 한다.
- `application.yaml`의 allowed origin은 실제 frontend origin에 맞춰 조정해야 한다.
