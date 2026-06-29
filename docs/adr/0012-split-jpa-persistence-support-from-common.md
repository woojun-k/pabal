# ADR-0012: JPA Persistence Support를 pabal-common에서 분리한다

> 상위 문서: [ADR 목록](README.md)  
> 관련 문서: [Pabal 공통 모듈 설계](../architecture/common-module-design.md), [Pabal 패키지 구조와 레이어](../architecture/package-structure-and-layers.md), [Pabal 멀티모듈 전환 전략](../architecture/multi-module-transition.md)

## Status

Accepted

## Context

`pabal-common`은 CQRS marker, event abstraction, 공통 contract, UUID v7 utility 같은 순수 shared kernel을 제공한다. 이전 구조에서는 JPA `@MappedSuperclass` base entity와 Hibernate UUID v7 generator도 `pabal-common`에 있었다.

Gradle 기준으로 `pabal-common`의 JPA/Hibernate 의존은 `compileOnly`였고 domain module compile classpath에 `jakarta.persistence`가 직접 전파되지는 않았다. 그러나 `pabal-common` 모듈 자체가 JPA support package를 포함하면 "common은 순수 primitive와 contract만 둔다"는 책임 경계가 흐려진다.

## Decision

JPA/Hibernate persistence support를 `pabal-persistence-support` 모듈로 분리한다.

- `pabal-common`은 JPA/Hibernate API에 의존하지 않는다.
- `pabal-persistence-support`는 `BaseEntity`, `UpdatableEntity`, `DeletableEntity`, `UuidV7Generated`, `UuidV7IdGenerator`를 소유한다.
- `pabal-persistence-support`는 UUID v7 utility 재사용을 위해 `pabal-common`에 의존할 수 있다.
- JPA Entity가 있는 infrastructure module만 `pabal-persistence-support`에 의존한다.
- Domain/application/API/contract 모듈은 `pabal-persistence-support`에 의존하지 않는다.

## Consequences

### Positive

- Shared kernel인 `pabal-common`에서 JPA/Hibernate 책임이 제거된다.
- Domain module이 persistence support module에 의존하지 않는 컴파일 경계를 명확히 할 수 있다.
- 향후 ArchUnit/Gradle dependency rule로 infrastructure-only dependency를 검증하기 쉬워진다.

### Negative

- infrastructure module마다 persistence support 의존을 명시해야 한다.
- UUID v7 JPA generator와 순수 UUID utility가 서로 다른 모듈에 나뉘므로 위치를 문서로 안내해야 한다.

### Follow-up

- [ ] module boundary 자동 검증에서 domain/application/API/contract → `pabal-persistence-support` 금지 규칙을 추가한다.
- [ ] 새 JPA support helper는 `pabal-common`이 아니라 `pabal-persistence-support`에 둔다.
