# ADR-0015: Bounded Context 간 조회 Contract를 pabal-common에서 pabal-integration-contract로 분리한다

> 상위 문서: [ADR 목록](README.md)
> 관련 문서: [Pabal 공통 모듈 설계](../architecture/common-module-design.md), [Pabal 패키지 구조와 레이어](../architecture/package-structure-and-layers.md), [Pabal 멀티모듈 전환 전략](../architecture/multi-module-transition.md), [ADR-0012](0012-split-jpa-persistence-support-from-common.md), [ADR-0010](0010-own-tenant-workspace-membership-in-separate-contexts.md)

## Status

Accepted

## Context

`TenantContract`, `WorkspaceContract`, `UserContract`와 이들이 노출하는 `UserInfo`, `WorkspaceMemberRole`은 bounded context가 다른 context의 내부 repository나 JPA entity에 직접 의존하지 않고 필요한 최소 조회를 수행하기 위한 cross-context integration port다([ADR-0010](0010-own-tenant-workspace-membership-in-separate-contexts.md) 참고). 이전 구조에서는 이 5개 타입이 `pabal-common`의 `com.polarishb.pabal.common.contract[.dto]` 패키지에 있었다.

`pabal-common`은 CQRS marker, event publisher abstraction, permission abstraction, UUID v7 utility 같은 순수 shared kernel을 표방하는 모듈이다. 그러나 Gradle 모듈 그래프 관점에서는 `pabal-common`이 모든 모듈에서 참조 가능하기 때문에, 실제로는 이 contract를 구현하지도 소비하지도 않는 모듈(`pabal-messenger-application`, 모든 `-domain`/`-api` 모듈)도 `pabal-common`을 통해 이 타입들을 우연히 참조할 수 있는 상태였다. 이는 "common은 최소 primitive만 둔다"는 책임 경계와, 실제 consumer만 의존해야 한다는 모듈 경계 원칙을 흐리게 한다. ADR-0012가 JPA/Hibernate persistence support를 `pabal-common`에서 분리한 것과 같은 종류의 문제다.

## Decision

Bounded context 간 조회 contract를 `pabal-integration-contract`라는 새 Gradle 모듈로 분리한다.

- `pabal-integration-contract`는 `TenantContract`, `WorkspaceContract`, `UserContract`(interface)와 `UserInfo`(record), `WorkspaceMemberRole`(enum)을 `com.polarishb.pabal.integration.contract[.dto]` 패키지 아래 소유한다.
- `pabal-integration-contract`는 project 의존이 없는 leaf 모듈이다(`java.util`만 참조).
- `pabal-common`은 `contract` 패키지를 갖지 않으며, `pabal-integration-contract`에 의존하지 않는다. 의존 방향은 consumer → `pabal-integration-contract`로 고정한다.
- production-scope 의존은 실제 provider/consumer만 선언한다: `pabal-tenant-application`, `pabal-user-application`, `pabal-workspace-application`(각 `TenantContractService`/`UserContractService`/`WorkspaceContractService`로 contract를 구현), `pabal-messenger-infrastructure`(`ContractRoomParticipantDirectoryAdapter`에서 소비).
- test-scope 의존은 main 소스에서는 참조하지 않지만 test 소스에서 타입을 참조하는 모듈에 명시한다: `pabal-user-infrastructure`, `pabal-workspace-infrastructure`.
- `pabal-messenger-application`과 모든 `-domain`/`-api` 모듈은 이 모듈에 의존하지 않는다.
- `checkProjectDependencyBoundaries`의 allow-map에 `pabal-integration-contract`를 빈 허용 집합으로 등록하고, 위 4개 production consumer의 허용 집합에만 추가한다.

## Consequences

### Positive

- `pabal-common`이 "모든 모듈이 우연히 접근 가능한 조회 contract 보관소"에서 다시 순수 shared kernel로 좁혀진다.
- 실제 provider/consumer만 `pabal-integration-contract`에 의존하므로, `pabal-messenger-application`이나 domain/api 모듈이 실수로 cross-context contract를 참조하는 경로가 컴파일 시점에 차단된다(`checkProjectDependencyBoundaries`).
- integration port가 독립 모듈로 분리되어, 향후 MSA 후보 분리 시 이 계약의 경계(동기 API/이벤트/캐시 전환 여부)를 별도로 판단하기 쉬워진다.

### Negative

- provider/consumer 모듈마다 새 모듈 의존을 명시해야 한다(`pabal-tenant-application`, `pabal-user-application`, `pabal-workspace-application`의 `api`, `pabal-messenger-infrastructure`의 `implementation`, 일부 infrastructure 모듈의 `testImplementation`).
- Gradle 모듈 수가 하나 늘어나 빌드 그래프 탐색 시 고려할 모듈이 증가한다.

### Follow-up

- [ ] 향후 새 cross-context 조회 contract를 추가할 때는 `pabal-common`이 아니라 `pabal-integration-contract`에 둔다.
- [ ] MSA 분리 검토 시 `pabal-integration-contract`의 조회 방식(동기 API/replicated read model/event projection 전환)을 [Pabal MSA 전환 준비 체크리스트](../architecture/msa-readiness-checklist.md)에서 다시 판단한다.

## Alternatives Considered

| Option | 장점 | 단점 | 결론 |
| --- | --- | --- | --- |
| `pabal-common`에 유지 | 추가 모듈 없음 | 모든 모듈이 우연히 접근 가능, 책임 경계 흐림 | 기각 |
| bounded context별 `-contract` 모듈에 분산 | context별 소유권 명확 | 3개 context가 서로의 contract를 참조해야 하는 순환 위험, provider/consumer가 이미 명확히 4개로 한정됨 | 기각 |
| 별도 공유 leaf 모듈(`pabal-integration-contract`) 신설 | 실제 consumer만 의존, `pabal-common`은 순수 shared kernel 유지, project 의존 없는 leaf 유지 | 모듈 하나 증가 | 채택 |
