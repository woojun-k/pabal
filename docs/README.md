# Pabal Project Docs

이 디렉터리는 Project Pabal의 공식 협업 문서 공간이다.

설계 결정은 `adr/`에 별도로 남긴다.

## 문서 운영 원칙

- 문서는 코드와 함께 PR로 리뷰한다.
- 설계 변경 PR에는 관련 문서 변경을 함께 포함한다.
- 아키텍처를 바꾸는 결정은 ADR로 남긴다.

## 현재 코드베이스 스냅샷

- 현재 상태: 단일 배포 멀티모듈 모놀리스
- 실행 모듈: `pabal-app`
- 공통 모듈: `pabal-common`, `pabal-security`
- Messenger 모듈: `pabal-messenger-domain`, `pabal-messenger-application`, `pabal-messenger-contract`, `pabal-messenger-api`, `pabal-messenger-infrastructure`
- 주요 구조: DDD + Hexagonal Architecture + CQRS + Realtime(STOMP)
- 장기 방향: 멀티모듈 모놀리스 안정화 이후 MSA 분리 가능성 검토

## 추천 읽기 순서

1. [Pabal 아키텍처 개요](architecture/overview.md)
2. [Pabal Messenger 온보딩 가이드](onboarding/messenger-onboarding.md)
3. [Pabal 패키지 구조와 레이어](architecture/package-structure-and-layers.md)
4. [Pabal 멀티모듈 전환 전략](architecture/multi-module-transition.md)
5. [Pabal 런타임 흐름](architecture/runtime-flow.md)
6. [Pabal 도메인 모델 상세](domain/messenger-domain-model.md)
7. [Pabal Command-Query 유스케이스 카탈로그](use-cases/command-query-catalog.md)
8. [Pabal Persistence 경계와 데이터 변환](architecture/persistence-boundary-and-mapping.md)
9. [Pabal 데이터베이스 스키마와 제약](architecture/database-schema-and-constraints.md)
10. [Pabal 이벤트 발행과 트랜잭션 경계](architecture/event-and-transaction-boundary.md)
11. [Pabal 보안과 JWT Claim 설계](security/jwt-claim-design.md)
12. [Pabal Realtime 이벤트 스키마](realtime/event-schema.md)
13. [Pabal 테스트 전략](testing/testing-strategy.md)
14. [ADR 목록](adr/README.md)

## 목차

### Architecture

- [아키텍처 개요](architecture/overview.md)
- [패키지 구조와 레이어](architecture/package-structure-and-layers.md)
- [런타임 흐름](architecture/runtime-flow.md)
- [크로스커팅 관심사](architecture/cross-cutting-concerns.md)
- [공통 모듈 설계](architecture/common-module-design.md)
- [Persistence 경계와 데이터 변환](architecture/persistence-boundary-and-mapping.md)
- [데이터베이스 스키마와 제약](architecture/database-schema-and-constraints.md)
- [이벤트 발행과 트랜잭션 경계](architecture/event-and-transaction-boundary.md)
- [로컬 개발과 런타임 구성](architecture/local-runtime.md)
- [Observability와 운영 설정](architecture/observability-and-operations.md)
- [멀티모듈 전환 전략](architecture/multi-module-transition.md)
- [MSA 전환 준비 체크리스트](architecture/msa-readiness-checklist.md)
- [기술 부채와 보강 목록](architecture/technical-debt.md)

### Domain / Use Cases

- [도메인 모델 상세](domain/messenger-domain-model.md)
- [Command-Query 유스케이스 카탈로그](use-cases/command-query-catalog.md)
- [HTTP API 예시와 오류 매핑](use-cases/http-api-and-error-mapping.md)
- [에러 코드와 예외 매핑표](use-cases/error-code-exception-mapping.md)
- [엔드포인트 시퀀스 다이어그램](use-cases/endpoint-sequence-diagrams.md)

### Realtime / Security / Testing

- [Realtime 이벤트 스키마](realtime/event-schema.md)
- [STOMP 연동 가이드](realtime/stomp-guide.md)
- [WebSocket 설정](realtime/websocket-configuration.md)
- [보안과 JWT Claim 설계](security/jwt-claim-design.md)
- [인가 경계와 멀티테넌시 체크포인트](security/authorization-and-multitenancy.md)
- [테스트 전략](testing/testing-strategy.md)
- [테스트 케이스 카탈로그](testing/test-case-catalog.md)

### Design / ADR / Governance

- [상세 설계 허브](design/design-hub.md)
- [ADR 목록](adr/README.md)
- [ADR 템플릿](adr/template.md)
- [문서 작성 가이드](contributing/docs-guide.md)
