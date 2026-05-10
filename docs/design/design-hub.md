---
tags:
  - pabal
  - detailed-design
  - hub
---

# Pabal 상세 설계 허브

> 상위 문서: [Pabal Wiki Home](../README.md)
> 관련 문서: [Pabal 아키텍처 개요](../architecture/overview.md), [Pabal 런타임 흐름](../architecture/runtime-flow.md), [Pabal Persistence 경계와 데이터 변환](../architecture/persistence-boundary-and-mapping.md), [Pabal 멀티모듈 전환 전략](../architecture/multi-module-transition.md)

이 문서는 실제 유지보수와 확장 작업에서 반복적으로 참조할 상세 설계 노트를 연결하는 허브다. 현재 문서 기준은 `pabal-app` 단일 배포와 `pabal-messenger-*` 멀티모듈 분리다.

## 핵심 상세 설계

- [Pabal 도메인 모델 상세](../domain/messenger-domain-model.md)
- [Pabal Command-Query 유스케이스 카탈로그](../use-cases/command-query-catalog.md)
- [Pabal 엔드포인트 시퀀스 다이어그램](../use-cases/endpoint-sequence-diagrams.md)
- [Pabal Persistence 경계와 데이터 변환](../architecture/persistence-boundary-and-mapping.md)
- [Pabal 데이터베이스 스키마와 제약](../architecture/database-schema-and-constraints.md)
- [Pabal 이벤트 발행과 트랜잭션 경계](../architecture/event-and-transaction-boundary.md)
- [Pabal Realtime 이벤트 스키마](../realtime/event-schema.md)
- [Pabal STOMP 연동 가이드](../realtime/stomp-guide.md)
- [Pabal 보안과 JWT Claim 설계](../security/jwt-claim-design.md)
- [Pabal 인가 경계와 멀티테넌시 체크포인트](../security/authorization-and-multitenancy.md)

## 아키텍처 전환 문서

- [Pabal 패키지 구조와 레이어](../architecture/package-structure-and-layers.md)
- [Pabal 공통 모듈 설계](../architecture/common-module-design.md)
- [Pabal 멀티모듈 전환 전략](../architecture/multi-module-transition.md)
- [Pabal MSA 전환 준비 체크리스트](../architecture/msa-readiness-checklist.md)
- [Pabal 크로스커팅 관심사](../architecture/cross-cutting-concerns.md)
- [Pabal 기술 부채와 보강 목록](../architecture/technical-debt.md)

## 운영형 문서

- [Pabal 로컬 개발과 런타임 구성](../architecture/local-runtime.md)
- [Pabal Observability와 운영 설정](../architecture/observability-and-operations.md)
- [Pabal HTTP API 예시와 오류 매핑](../use-cases/http-api-and-error-mapping.md)
- [Pabal 에러 코드와 예외 매핑표](../use-cases/error-code-exception-mapping.md)
- [Websocket 설정](../realtime/websocket-configuration.md)
- [Pabal 테스트 전략](../testing/testing-strategy.md)
- [Pabal 테스트 케이스 카탈로그](../testing/test-case-catalog.md)

## 설계 판단 기준

- API request/response는 외부 계약이고, application command/query/result는 유스케이스 입력/출력이다.
- repository port와 realtime port는 `pabal-messenger-application`의 outbound port다.
- domain은 `State`, `Persisted*`, JPA Entity를 알지 않는다.
- infrastructure는 application port를 구현하고 JPA/STOMP/WebSocket security 세부사항을 가진다.
- contract는 persistence/realtime 경계 shape를 안정화하되 비즈니스 규칙을 소유하지 않는다.
- `pabal-common`에는 여러 모듈이 공유하는 최소 primitive만 둔다.
- runtime resource와 Flyway migration은 `pabal-app`이 소유한다.

## 문서 항목 지도

| 질문 | 먼저 볼 문서 |
| --- | --- |
| 로컬에서 어떻게 실행하는가? | [Pabal 로컬 개발과 런타임 구성](../architecture/local-runtime.md) |
| 모듈 책임은 어떻게 나뉘는가? | [Pabal 패키지 구조와 레이어](../architecture/package-structure-and-layers.md), [Pabal 공통 모듈 설계](../architecture/common-module-design.md) |
| DB schema와 constraint는 어디서 관리하는가? | [Pabal 데이터베이스 스키마와 제약](../architecture/database-schema-and-constraints.md) |
| Domain model과 JPA Entity는 어떻게 분리되는가? | [Pabal Persistence 경계와 데이터 변환](../architecture/persistence-boundary-and-mapping.md) |
| 이벤트는 언제 발행되고 realtime으로 어떻게 나가는가? | [Pabal 이벤트 발행과 트랜잭션 경계](../architecture/event-and-transaction-boundary.md), [Pabal Realtime 이벤트 스키마](../realtime/event-schema.md) |
| STOMP 연결/인가 문제는 어디서 확인하는가? | [Websocket 설정](../realtime/websocket-configuration.md), [Pabal STOMP 연동 가이드](../realtime/stomp-guide.md) |
| trace id와 local observability는 어디서 보는가? | [Pabal Observability와 운영 설정](../architecture/observability-and-operations.md) |
| 어떤 테스트를 어디에 추가하는가? | [Pabal 테스트 전략](../testing/testing-strategy.md), [Pabal 테스트 케이스 카탈로그](../testing/test-case-catalog.md) |
