# ADR-0002: MSA보다 멀티모듈 모놀리스 안정화를 우선한다

## Status

Accepted

## Context

Pabal Messenger는 현재 `pabal-app`을 실행 모듈로 사용하는 단일 배포 구조다. 내부적으로는 `pabal-common`, `pabal-security`, `pabal-messenger-domain`, `pabal-messenger-application`, `pabal-messenger-contract`, `pabal-messenger-api`, `pabal-messenger-infrastructure`로 모듈을 나누고 있다.

Messenger API/Application/Domain, Realtime Gateway 등은 장기적으로 분리 후보가 될 수 있다. 하지만 현재 단계에서 MSA로 전환하면 다음 비용이 먼저 발생한다.

- 서비스 간 통신 계약 관리
- 분산 트랜잭션 또는 eventual consistency 설계
- 배포/관측/장애 대응 복잡도 증가
- 테스트와 로컬 개발 환경 복잡도 증가
- 아직 안정화되지 않은 모듈 경계의 조기 고착화

## Decision

현재 목표는 MSA가 아니라 멀티모듈 모놀리스 안정화로 둔다.

- 단일 배포를 유지한다.
- Gradle module 경계와 의존 방향을 명확히 한다.
- domain/application/api/infrastructure/contract 책임을 분리한다.
- MSA 전환은 모듈 경계, 데이터 소유권, 외부 계약, 운영 기준이 안정화된 뒤 검토한다.

## Consequences

### Positive

- 분산 시스템 비용 없이 코드 구조의 경계를 먼저 검증할 수 있다.
- 리팩터링 속도를 유지하면서 모듈별 책임을 선명하게 만들 수 있다.
- MSA 전환 시에도 분리 후보와 계약을 더 명확히 판단할 수 있다.

### Negative

- 런타임 장애 격리는 아직 서비스 단위로 나뉘지 않는다.
- 특정 모듈의 부하만 독립적으로 스케일링하기 어렵다.
- 모듈 경계 검증을 자동화하지 않으면 시간이 지나며 경계가 흐려질 수 있다.

### Follow-up

- [ ] 모듈 의존 규칙 자동 검증을 추가한다.
- [ ] MSA 전환 후보별 데이터 소유권과 외부 계약을 문서화한다.
- [ ] Realtime Gateway 분리 조건을 별도 체크리스트로 관리한다.
