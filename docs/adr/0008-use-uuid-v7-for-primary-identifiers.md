# ADR-0008: 기본 식별자는 UUID v7을 사용한다

## Status

Accepted

## Context

Pabal은 여러 모듈과 도메인 객체에서 전역적으로 충돌 가능성이 낮은 식별자가 필요하다. 동시에 DB index locality와 시간 기반 정렬 가능성도 중요하다.

기존 auto-increment 방식은 단일 DB 내부에서는 단순하지만, 모듈 분리 또는 장기적인 서비스 분리 가능성을 고려하면 전역 ID 전략으로는 제약이 있다.

## Decision

기본 식별자는 UUID v7을 사용한다.

- Java/JPA의 기본 생성 경로는 `UuidV7IdGenerator`와 `@UuidV7Generated`로 둔다.
- monotonic generation이 필요한 JPA entity에서는 `UuidV7Generated.Mode.MONOTONIC`을 사용한다.
- DB에는 `uuidv7()` 함수를 fallback/default로 둔다.
- 수동 SQL, 운영 보정, 테스트 데이터 생성 시 DB default를 보조 안전장치로 사용한다.

## Consequences

### Positive

- 전역적으로 충돌 가능성이 낮은 ID를 사용할 수 있다.
- UUID v4보다 시간 순서 기반 정렬과 index locality에 유리하다.
- 애플리케이션과 DB 양쪽에 생성 경로를 둘 수 있다.

### Negative

- ID 생성 규칙이 프로젝트 공통 모듈에 의존한다.
- DB fallback과 Java generator가 서로 다른 구현이므로 테스트로 정합성을 확인해야 한다.
- 사람이 읽기 쉬운 순차 ID보다 디버깅 가독성은 낮다.

### Follow-up

- [ ] Java generator와 DB `uuidv7()`의 version/variant/timestamp 검증 테스트를 유지한다.
- [ ] 외부 공개 ID와 내부 PK를 분리할 필요가 있는 도메인을 별도 검토한다.
