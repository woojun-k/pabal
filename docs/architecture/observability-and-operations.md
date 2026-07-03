---
tags:
  - pabal
  - architecture
  - observability
  - operations
---

# Pabal Observability와 운영 설정

> 상위 문서: [Pabal Wiki Home](../README.md)
> 관련 문서: [Pabal 로컬 개발과 런타임 구성](local-runtime.md), [Pabal 크로스커팅 관심사](cross-cutting-concerns.md), [Pabal 공통 모듈 설계](common-module-design.md), [Pabal 에러 코드와 예외 매핑표](../use-cases/error-code-exception-mapping.md), [Websocket 설정](../realtime/websocket-configuration.md)

## 개요

Layer: App / Common / Infrastructure
Status: Partial

현재 코드베이스에는 Actuator health probe, 핵심 message send metric, local observability 구성, API 오류 trace id 연결, production 실행 profile이 구현되어 있다. 운영 exporter, alerting, dashboard, log correlation 정책은 아직 별도 운영 설계가 필요하다.

현재 구현된 축은 다음이다.

- Spring Boot Actuator
- liveness/readiness health probe
- message send business metric
- Spring Boot OpenTelemetry starter
- OpenTelemetry API 기반 trace id 추출
- local OTel collector
- `GlobalExceptionHandler`의 traceId 포함 오류 응답
- local profile의 WebSocket/Security/Flyway debug logging
- `prod` profile의 datasource/JWT issuer/Redis/STOMP relay 설정

## Actuator

Layer: App / Security

`pabal-app`은 `spring-boot-starter-actuator`를 사용한다. `SecurityConfig`는 `/actuator/health`와 `/actuator/health/**`를 인증 없이 접근 가능하게 둔다. `metrics` endpoint는 노출되지만 health와 달리 인증 없이 열지 않는다.

기본 application 설정은 다음 endpoint만 web exposure에 포함한다.

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

## Health probe

Layer: App / Security

Kubernetes 등 orchestrator가 사용할 probe endpoint는 다음이다.

| 용도 | Endpoint | 포함 health indicator |
| --- | --- | --- |
| liveness | `/actuator/health/liveness` | `livenessState` |
| readiness | `/actuator/health/readiness` | `readinessState`, `db`, `redis` |

`management.endpoint.health.probes.enabled=true`로 probe group을 명시적으로 켠다. readiness는 PostgreSQL과 Redis가 준비되지 않으면 `DOWN`으로 내려가 traffic 수용을 막는다. liveness는 외부 의존성 대신 JVM/application liveness state만 본다.

local profile에서는 Redis health가 켜져 있다.

```yaml
management:
  health:
    redis:
      enabled: true
```

기본 profile에서도 Redis health는 켜져 있으므로 readiness는 local/prod 모두 Redis 상태를 반영한다.

## Business metric

Layer: Application / Infrastructure

`SendMessageCommandHandler`는 application outbound port인 `MessageSendMetrics`를 호출한다. Infrastructure의 `MicrometerMessageSendMetricsAdapter`는 다음 counter를 기록한다.

| Metric | Type | Tags | 의미 |
| --- | --- | --- | --- |
| `pabal.messenger.message.send.total` | counter | `outcome=sent` | 신규 message send 성공 |
| `pabal.messenger.message.send.total` | counter | `outcome=duplicate` | `clientMessageId` 기반 idempotent duplicate 처리 |
| `pabal.messenger.message.send.total` | counter | `outcome=failed` | send command가 예외로 실패 |

`tenantId`, `chatRoomId`, `senderId`, `messageId`는 high-cardinality 값이므로 metric tag에 넣지 않는다. 성공률은 `sent / (sent + duplicate + failed)`, 실패율은 `failed / (sent + duplicate + failed)`로 계산한다.

## Production profile

Layer: App / Security / Infrastructure

`application-prod.yaml`은 production 실행에 필요한 외부 의존성을 env var로 명시한다. 기본값이 없는 항목은 누락 시 startup 단계에서 실패하도록 둔다.

필수 env var:

| 범주 | Env var |
| --- | --- |
| PostgreSQL | `PABAL_DATASOURCE_URL`, `PABAL_DATASOURCE_USERNAME`, `PABAL_DATASOURCE_PASSWORD` |
| JWT issuer | `PABAL_JWT_ISSUER_URI` |
| Redis | `PABAL_REDIS_HOST` |
| STOMP relay | `PABAL_STOMP_RELAY_HOST`, `PABAL_STOMP_CLIENT_LOGIN`, `PABAL_STOMP_CLIENT_PASSCODE`, `PABAL_STOMP_SYSTEM_LOGIN`, `PABAL_STOMP_SYSTEM_PASSCODE` |
| WebSocket CORS | `PABAL_WEBSOCKET_ALLOWED_ORIGIN_PATTERNS` |

주요 optional env var:

| Env var | 기본값 |
| --- | --- |
| `PABAL_REDIS_PORT` | `6379` |
| `PABAL_REDIS_CONNECT_TIMEOUT` | `2s` |
| `PABAL_REDIS_TIMEOUT` | `2s` |
| `PABAL_REDIS_SSL_ENABLED` | `false` |
| `PABAL_JWT_AUDIENCE` | `pabal-api` |
| `PABAL_STOMP_RELAY_PORT` | `61613` |
| `PABAL_WEBSOCKET_ENDPOINT_PATH` | `/websocket` |
| `PABAL_WEBSOCKET_SOCK_JS_ENABLED` | `false` |

`prod` profile은 `spring.docker.compose.enabled=false`, `spring.jpa.hibernate.ddl-auto=validate`, `spring.sql.init.mode=never`를 사용한다.

## External dependency failure policy

Layer: App / Infrastructure / Security

| Dependency | 정책 |
| --- | --- |
| PostgreSQL | 필수. datasource env var 누락, 연결 실패, Flyway 실패, schema validation 실패는 startup fail-fast로 처리한다. 실행 중 장애는 readiness `DOWN`으로 traffic을 제거한다. |
| JWT issuer | 필수. `prod` profile은 issuer 기반 `JwtDecoder`를 구성하며 issuer discovery 실패는 startup 실패로 본다. |
| Redis | refresh token replay cache와 readiness에 사용한다. cache read/write 실패는 DB truth를 유지하기 위해 request 자체를 실패시키지 않지만 readiness는 `DOWN`으로 내려 운영자가 장애를 볼 수 있게 한다. |
| STOMP relay | production realtime fanout의 필수 dependency다. relay 접속 장애 시 WebSocket message fanout이 실패할 수 있으므로 message send metric, WebSocket log, relay broker alert를 함께 확인한다. |

운영 장애 시 우선순위:

1. `/actuator/health/readiness`에서 `db`/`redis` 상태를 확인한다.
2. `pabal.messenger.message.send.total{outcome="failed"}` 증가 여부를 확인한다.
3. client가 받은 API error `traceId`로 application log/trace를 추적한다.
4. STOMP relay 장애가 의심되면 relay broker connection/heartbeat와 `com.polarishb.pabal.messenger.infrastructure.realtime.ws` 로그를 확인한다.

## OpenTelemetry 구성

Layer: App / Common / Infrastructure

사용 dependency:

- `spring-boot-starter-opentelemetry`
- `opentelemetry-api`

local compose의 collector:

| 항목 | 값 |
| --- | --- |
| Image | `otel/opentelemetry-collector-contrib:0.151.0` |
| OTLP HTTP | `0.0.0.0:4318` |
| OTLP gRPC | `0.0.0.0:4317` |
| health_check | `0.0.0.0:13133` |
| exporter | `debug`, `verbosity: detailed` |
| pipelines | traces, metrics, logs |

Collector config는 `docker/otel/otel-collector.local.yaml`에 있다.

## API error trace id

Layer: Web / Common

`pabal-web`의 `GlobalExceptionHandler`는 `ApiError.traceId`를 채운다.

```text
Span.current().getSpanContext().getTraceId()
또는
MDC.get("traceId")
```

오류 logging 시 `withTraceId`가 MDC에 trace id를 넣어 public error response와 internal log를 연결한다.

```text
GlobalExceptionHandler
→ currentTraceId
→ ApiError.of(... traceId ...)
→ logApiError
→ MDC traceId 설정 후 warn/error/debug log
```

이 설계 덕분에 client가 받은 `traceId`를 서버 로그 또는 trace 시스템에서 추적할 수 있다.

## Logging 설정

Layer: App / Infrastructure

`application-local.yaml`은 개발 편의를 위해 다음 로그 레벨을 높인다.

- `org.springframework.web.socket: DEBUG`
- `org.springframework.messaging: DEBUG`
- `org.springframework.security: DEBUG`
- `com.polarishb.pabal.messenger.infrastructure.realtime.ws: TRACE`
- `com.polarishb.pabal.messenger.infrastructure.config: TRACE`
- `com.polarishb.pabal.security: TRACE`
- `org.flywaydb: DEBUG`
- `org.springframework.boot.flyway: DEBUG`

운영 profile에서는 이 레벨을 그대로 사용하면 민감 정보나 과도한 로그가 발생할 수 있으므로 별도 조정이 필요하다.

## WebSocket 관측 포인트

Layer: Infrastructure / Security

WebSocket 문제를 조사할 때 우선 확인할 지점은 다음이다.

- STOMP CONNECT token 추출: `StompConnectAuthenticationInterceptor`
- 인증 manager wiring: `WebSocketAuthenticationManagerConfig`
- principal 변환: `PabalJwtAuthenticationConverter`
- SUBSCRIBE authorization: `RoomSubscriptionAuthorizationManager`
- outbound destination: `ChatRealtimeDestinations`
- 실제 전송 adapter: `StompChatRealtimeAdapter`

연동 세부사항은 [Websocket 설정](../realtime/websocket-configuration.md)과 [Pabal STOMP 연동 가이드](../realtime/stomp-guide.md)를 기준으로 본다.

## 운영 보강 후보

Status: Planned

- production exporter 결정
- trace/log correlation format 표준화
- WebSocket CONNECT/SUBSCRIBE 실패 metric
- realtime publish 실패 metric과 retry 정책
- Flyway migration 실패 alert

## 점검 체크리스트

- [ ] `/actuator/health/liveness`가 기대한 status를 반환하는가?
- [ ] `/actuator/health/readiness`가 PostgreSQL/Redis 상태를 반영하는가?
- [ ] `pabal.messenger.message.send.total` metric이 `sent`/`duplicate`/`failed` outcome으로 기록되는가?
- [ ] local collector가 4317/4318을 열고 있는가?
- [ ] API 오류 응답에 `traceId`가 포함되는가?
- [ ] 서버 로그에서 같은 `traceId`를 찾을 수 있는가?
- [ ] WebSocket 실패 시 CONNECT 인증 로그와 SUBSCRIBE 인가 로그를 분리해서 볼 수 있는가?
- [ ] 운영 profile에서 DEBUG/TRACE 로그가 과도하게 켜져 있지 않은가?
