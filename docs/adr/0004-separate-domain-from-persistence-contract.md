# ADR-0004: 도메인 모델과 Persistence/JPA 모델을 분리한다

## Status

Accepted

## Context

도메인 모델이 JPA entity, persistence state, database schema shape를 직접 알게 되면 도메인 규칙과 저장소 구현이 결합된다. 이 경우 schema 변경, query 최적화, migration, JPA annotation 변경이 domain에 직접 영향을 준다.

Pabal Messenger는 domain model, persistence contract, JPA entity를 분리해 domain 순수성을 유지하려 한다.

## Decision

도메인 모델과 persistence/JPA 모델을 분리한다.

- Domain은 `State`, `Persisted*`, JPA Entity를 import하지 않는다.
- Repository port는 application outbound port에 둔다.
- Persistence contract는 `pabal-messenger-contract`에서 관리한다.
- JPA entity와 Spring Data repository는 infrastructure에 둔다.
- 도메인 객체와 저장 모델 간 변환은 명시적인 mapper를 통해 수행한다.

## Consequences

### Positive

- 도메인 모델이 저장소 기술에 종속되지 않는다.
- DB schema와 domain invariant를 분리해서 관리할 수 있다.
- persistence 최적화 또는 저장소 교체 가능성이 높아진다.
- application 테스트에서 repository port를 쉽게 대체할 수 있다.

### Negative

- 변환 코드와 contract 타입이 추가된다.
- domain과 DB constraint의 정책 불일치를 지속적으로 점검해야 한다.
- 조회 모델이 복잡해질수록 mapping 비용이 증가한다.

### Follow-up

- [ ] domain validation과 DB constraint의 정책 불일치를 테스트로 보강한다.
- [ ] `message.content` 길이 정책처럼 중복 검증되는 항목은 문서와 migration을 함께 관리한다.
- [ ] persistence mapper 변경 시 test fixture를 함께 갱신한다.
