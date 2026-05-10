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

현재 코드베이스에는 local observability 구성과 API 오류 trace id 연결이 구현되어 있다. 운영 exporter, alerting, dashboard, log correlation 정책은 아직 별도 운영 설계가 필요하다.

현재 구현된 축은 다음이다.

- Spring Boot Actuator
- Spring Boot OpenTelemetry starter
- OpenTelemetry API 기반 trace id 추출
- local OTel collector
- `GlobalExceptionHandler`의 traceId 포함 오류 응답
- local profile의 WebSocket/Security/Flyway debug logging

## Actuator

Layer: App / Security

`pabal-app`은 `spring-boot-starter-actuator`를 사용한다. `SecurityConfig`는 `/actuator/health`를 인증 없이 접근 가능하게 둔다.

local profile에서는 Redis health가 켜져 있다.

```yaml
management:
  health:
    redis:
      enabled: true
```

현재 문서 기준으로 health endpoint는 readiness 확인과 local wiring 점검 용도다.

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

Layer: Common

`GlobalExceptionHandler`는 `ApiError.traceId`를 채운다.

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
- actuator exposure 범위 정의
- WebSocket CONNECT/SUBSCRIBE 실패 metric
- realtime publish 실패 metric과 retry 정책
- Flyway migration 실패 alert
- PostgreSQL/Redis health check를 readiness/liveness로 분리할지 결정

## 점검 체크리스트

- [ ] `/actuator/health`가 기대한 status를 반환하는가?
- [ ] local collector가 4317/4318을 열고 있는가?
- [ ] API 오류 응답에 `traceId`가 포함되는가?
- [ ] 서버 로그에서 같은 `traceId`를 찾을 수 있는가?
- [ ] WebSocket 실패 시 CONNECT 인증 로그와 SUBSCRIBE 인가 로그를 분리해서 볼 수 있는가?
- [ ] 운영 profile에서 DEBUG/TRACE 로그가 과도하게 켜져 있지 않은가?
