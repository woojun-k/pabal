# ADR 목록

ADR(Architecture Decision Record)은 프로젝트의 주요 설계 결정을 기록한다. 코드를 봐도 의도가 바로 드러나지 않는 결정, 되돌리기 어려운 결정, 팀 합의가 필요한 결정은 ADR로 남긴다.

## 작성 규칙

- 파일명: `NNNN-short-decision-title.md`
- 번호는 한 번 부여하면 재사용하지 않는다.
- 상태는 `Proposed`, `Accepted`, `Deprecated`, `Superseded` 중 하나를 사용한다.
- 기존 결정을 대체하면 새 ADR에서 `Supersedes` 또는 `Superseded by`를 명시한다.

## 현재 ADR

| ADR | Status | Decision |
| --- | --- | --- |
| [0001](0001-manage-docs-in-repository.md) | Accepted | 공식 협업 문서는 레포의 `docs/`에서 관리한다. |
| [0002](0002-use-modular-monolith-before-msa.md) | Accepted | MSA보다 멀티모듈 모놀리스 안정화를 우선한다. |
| [0003](0003-adopt-ddd-hexagonal-cqrs-boundaries.md) | Accepted | Messenger는 DDD + Hexagonal + CQRS 경계를 따른다. |
| [0004](0004-separate-domain-from-persistence-contract.md) | Accepted | 도메인 모델과 persistence/JPA 모델을 분리한다. |
| [0005](0005-publish-domain-events-after-commit.md) | Accepted | Realtime outbound는 after-commit domain event 기반으로 발행한다. |
| [0006](0006-use-jwt-principal-for-tenant-user-scope.md) | Accepted | JWT principal을 tenant/user scope의 기준으로 사용한다. |
| [0007](0007-use-stomp-for-current-realtime-contract.md) | Accepted | 현재 realtime contract는 WebSocket/STOMP를 기준으로 둔다. |
| [0008](0008-use-uuid-v7-for-primary-identifiers.md) | Accepted | 기본 식별자는 UUID v7을 사용한다. |
