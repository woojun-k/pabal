# ADR-0013: TenantRegistration의 단일 `expiresAt`을 verification/activation 두 시각으로 분리하고 lapsed 상태를 명시한다

## Status

Accepted

## Context

`TenantRegistration` domain model(`pabal-tenant-domain`)은 원래 단일 `expiresAt` 필드와 4개 status(`PENDING_VERIFICATION`, `VERIFIED`, `ACTIVATED`, `EXPIRED`)로 두 개의 서로 다른 시간 제약을 표현하고 있었다.

- `PENDING_VERIFICATION` 상태에서 DNS TXT 검증을 완료해야 하는 마감(verification window).
- `VERIFIED` 상태에서 실제 tenant 활성화(`activate()`)를 완료해야 하는 마감(activation window).

하나의 `expiresAt` 필드가 두 서로 다른 deadline 의미를 겸하면서 다음 문제가 있었다.

- `activate()`가 `expiresAt`을 기준으로 검사하는지, 그 필드가 verification 기준인지 activation 기준인지 코드만 봐서는 모호했다.
- `VERIFIED` 상태로 전이된 뒤 activation이 무기한 허용되는 사실상의 unbounded window였다. `activate()` 자체는 상태만 확인하고 시간 제약 없이 통과할 수 있었다.
- activation window가 지나 활성화가 더는 허용되지 않아야 하는 상황을 별도 status로 관측할 방법이 없었다. registration이 "복구 가능한 지연 상태"인지 "완전히 닫힌 상태"인지 구분되지 않았다.

이 경계를 closing하기 위해 `pabal-tenant-domain`/`pabal-tenant-contract` 범위에서 `TenantRegistration`, `TenantRegistrationStatus`, `TenantRegistrationSnapshot`, `TenantRegistrationState`를 수정했다. `pabal-tenant-infrastructure`(JPA entity, Flyway V10 migration, scheduler)와 `pabal-tenant-application`/`pabal-tenant-api`(two-phase 검증 handler, TTL 설정)는 이번 변경 범위 밖의 후속 작업으로 명시적으로 남겨두었고, 이후 완료되어 현재는 domain/contract/persistence/application/api 전 계층이 분리된 시간/상태 모델로 정합한다.

## Decision

`TenantRegistration`의 시간/상태 모델을 다음과 같이 분리한다.

- 단일 `expiresAt`을 `verificationExpiresAt`(PENDING_VERIFICATION → 검증 마감)과 `activationExpiresAt`(DOMAIN_VERIFIED → 활성화 마감)으로 분리한다.
- status를 5개로 확장한다: `PENDING_VERIFICATION`, `DOMAIN_VERIFIED`(기존 `VERIFIED`를 rename), `REVERIFICATION_REQUIRED`(신규), `ACTIVATED`, `EXPIRED`.
- `activate()`는 `DOMAIN_VERIFIED` 상태에서만 허용하고, `activatedAt`이 `activationExpiresAt` 이상이면 `TenantRegistrationExpiredException`을 던진다. 즉 activation에도 명시적 마감이 생긴다.
- `requireReverification(now)`는 `DOMAIN_VERIFIED` 상태에서 activation window가 실제로 지났을 때만(`!now.isBefore(activationExpiresAt)`) `REVERIFICATION_REQUIRED`로 전이한다. window가 아직 열려 있으면 `IllegalStateException`을 던져 caller/scheduler 버그를 드러낸다.
- `reverify(reverifiedAt, activationExpiresAt)`는 `REVERIFICATION_REQUIRED`에서만 허용되고, 새 `activationExpiresAt`을 설정하며 `DOMAIN_VERIFIED`로 복귀한다.
- `REVERIFICATION_REQUIRED`는 `EXPIRED`와 구분되는 **복구 가능한(non-terminal)** 상태다. `isOpen()`은 `REVERIFICATION_REQUIRED`를 포함해 true를 반환한다. terminal 상태는 여전히 `EXPIRED` 하나뿐이다.
- `TenantRegistrationSnapshot`(domain)과 `TenantRegistrationState`(contract)는 두 timestamp 필드를 모두 보관하며 `snapshot()`/`reconstitute()` round-trip을 유지한다.

### 일반화 원칙 (같은 ADR에 포함)

이 결정은 `TenantRegistration`에만 국한되지 않는 일반 원칙을 함께 남긴다. 별도 ADR로 분리하지 않고 이 ADR의 하위 절로 기록하기로 했다 — 이 프로젝트에서 현재 이 원칙을 요구하는 구체 사례가 `TenantRegistration` 하나뿐이고, 그 사례에서 곧바로 도출된 결론이라 문서를 분리하면 오히려 맥락이 끊어지기 때문이다. 이후 두 번째 이상의 사례가 나오면 그때 일반 원칙만 다루는 별도 ADR로 승격하는 것을 권장한다.

> 하나의 `~At` 타임스탬프(또는 하나의 status)가 서로 다른 여러 의미/마감을 동시에 표현하게 되면, 그 필드/상태는 의미별로 분리해야 한다.

근거:

- 겹쳐진 의미는 코드 리뷰와 도메인 테스트만으로는 어떤 마감이 어떤 전이에 적용되는지 드러나지 않는다.
- 겹쳐진 의미 중 하나에만 boundary check(`activate()`의 만료 검사 같은)가 구현되면, 나머지 의미는 암묵적으로 unbounded가 되어 버그가 아니라 "설계상 빠진 것"으로 남는다.
- 분리된 필드/상태는 각각 독립적인 domain invariant(예: `activationExpiresAt.isAfter(verifiedAt)`)와 독립적인 실패 모드(`TenantRegistrationExpiredException` vs `IllegalStateException`)를 가질 수 있다.
- 이 원칙을 적용할 때 새로 생기는 상태가 기존 terminal 상태와 혼동되지 않도록, "복구 가능한 지연 상태"와 "terminal 상태"를 status enum 수준에서도 구분해야 한다(`REVERIFICATION_REQUIRED` vs `EXPIRED`).

## Consequences

### Positive

- `activate()`의 unbounded activation 경계가 닫혔다. activation window가 지나면 `activate()`는 항상 `TenantRegistrationExpiredException`을 던진다.
- lapsed activation window가 `REVERIFICATION_REQUIRED`라는 명시적 상태로 관측 가능해졌다. 이전에는 이 상태를 구분할 방법이 없었다.
- `verificationExpiresAt`/`activationExpiresAt` 각각의 invariant(`request`, `markVerified`, `reverify`에서의 순서 검증)가 독립적으로 강제된다.
- domain 순수성(불변 모델, 주입된 시간, `State`/`Persisted*` 미의존)을 유지한 채로 이루어진 변경이다.

### Negative

- status가 4개에서 5개로 늘어 status를 다루는 모든 switch/if 분기(application/infrastructure 포함)가 `REVERIFICATION_REQUIRED`를 명시적으로 처리해야 한다.

## Alternatives Considered

| Option | 장점 | 단점 | 결론 |
| --- | --- | --- | --- |
| 단일 `expiresAt` 유지, `activate()`에만 별도 필드로 마감 추가 | 변경 범위가 작음 | 필드명과 실제 의미가 계속 어긋나고, 겹쳐진 의미가 다음 상태 추가 시 또 반복됨 | 채택하지 않음 |
| status만 5개로 늘리고 timestamp는 그대로 둠 | timestamp 마이그레이션 불필요 | activation deadline을 표현할 필드가 없어 lapsed 감지 자체가 불가능 | 채택하지 않음 |
| timestamp 분리 + status 확장(채택안) | 각 전이의 마감과 상태가 1:1로 대응, lapsed 상태가 관측 가능 | persistence/application/api 동시 갱신이 필요해 후속 작업이 생김 | 채택 |

## 관련 문서

- [Pabal 기술 부채와 보강 목록](../architecture/technical-debt.md)
- [Pabal Command-Query 유스케이스 카탈로그](../use-cases/command-query-catalog.md)
- [Pabal 데이터베이스 스키마와 제약](../architecture/database-schema-and-constraints.md)
- [ADR-0004: 도메인 모델과 Persistence/JPA 모델을 분리한다](0004-separate-domain-from-persistence-contract.md)
