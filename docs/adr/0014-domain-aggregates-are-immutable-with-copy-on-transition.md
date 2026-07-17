# ADR-0014: Domain aggregate는 immutable하게 두고 상태 전이는 새 instance를 반환한다

## Status

Accepted

## Context

각 bounded context의 domain aggregate(`ChatRoom`, `ChatRoomMember`, `Message`, `DirectChatMapping`, `Workspace`, `WorkspaceMember`, `User`, `Tenant`, `TenantRegistration`)는 상태 전이를 domain 메서드로 표현한다. 이때 두 가지 스타일이 혼재할 수 있다.

- **copy-on-transition**: 필드를 `private final`로 두고, 상태 전이 메서드는 receiver를 바꾸지 않고 변경된 필드만 반영한 새 aggregate instance를 반환한다.
- **in-place mutation**: 필드를 mutable하게 두고, 상태 전이 메서드는 `void`로 receiver의 필드를 직접 재할당한다.

대부분의 aggregate(`ChatRoomMember`, `Message`, `ChatRoom`, `User`, `TenantRegistration`)는 이미 copy-on-transition을 따르고 있었으나, `WorkspaceMember`의 `leave()`/`changeRole()`만 `void` in-place mutation이었다. 또한 전이 메서드가 없는 `Tenant`/`Workspace`/`DirectChatMapping`은 필드가 non-final이라 스타일상으로도 일관되지 않았다.

이 불일치는 실질적인 위험을 만든다.

- 같은 계층·같은 종류의 aggregate가 서로 다른 전이 규약을 가지면, application handler가 반환값을 저장해야 하는지(copy) receiver를 저장하면 되는지(in-place)를 aggregate마다 다르게 기억해야 한다.
- in-place mutation은 전이 도중 예외가 나면 partial mutation을 남길 수 있고, 이를 막기 위한 방어 코드/테스트가 별도로 필요하다.
- non-final 필드는 지금 mutator가 없더라도, 이후 전이 메서드가 추가될 때 in-place mutation으로 흘러가기 쉬운 여지를 남긴다.

## Decision

모든 domain aggregate는 immutable하게 두고, 상태 전이는 copy-on-transition으로 표현한다.

- aggregate의 모든 필드는 `private final`로 선언한다.
- 상태 전이 메서드는 `void`로 receiver를 변경하지 않는다. 대신 변경된 필드만 반영한 **새 aggregate instance를 반환**하고, 나머지 필드는 receiver 값을 그대로 복사한다.
- 전이가 거부되면(invariant 위반) 새 instance를 만들지 않고 domain exception을 던진다. receiver는 어떤 경우에도 변경되지 않는다.
- 전이가 no-op(예: 같은 role로의 `changeRole`, 이미 배정된 sequence로의 `assignSequence`)이면 receiver instance를 그대로 반환할 수 있다. 이때 `updatedAt` 등 파생 필드도 바꾸지 않는다.
- 전이에 필요한 시간은 caller-supplied `Instant`로 주입받는다. domain은 전이 내부에서 `Instant.now()`를 호출하지 않는다(기존 규약과 동일).
- equality/hashCode는 계속 identity(`id`) 기준을 유지한다. copy-on-transition으로 반환된 instance는 receiver와 `id`가 같으므로 동등하게 취급된다.

이 규약은 신규 aggregate와 신규 전이 메서드에도 동일하게 적용한다.

## Consequences

### Positive

- 모든 aggregate가 동일한 전이 규약을 따르므로, application 계층은 "전이 메서드의 반환값을 저장한다"는 단일 규칙만 기억하면 된다.
- 전이 중 예외가 나도 partial mutation이 불가능하다. immutability가 원자성을 구조적으로 보장한다.
- `final` 필드는 이후 전이 메서드가 추가될 때 in-place mutation을 컴파일 단계에서 막는다.
- virtual thread/동시성 환경에서 aggregate instance 공유가 안전해진다.

### Negative

- 필드가 많은 aggregate는 전이 메서드마다 전체 필드를 넘기는 생성자 호출이 반복된다(현재는 Lombok `@AllArgsConstructor(access = PRIVATE)`로 흡수). 필드 추가 시 각 전이 메서드의 생성자 호출도 함께 갱신해야 한다.
- caller가 반환값을 무시하면 전이가 유실된다. in-place mutation과 달리 "메서드만 호출하면 반영"되지 않으므로, 반환값 사용이 계약의 일부임을 테스트로 고정해야 한다.

## Alternatives Considered

| Option | 장점 | 단점 | 결론 |
| --- | --- | --- | --- |
| in-place mutation으로 통일 | 반환값을 다룰 필요 없음, 필드 많은 aggregate에서 생성자 반복 없음 | partial mutation 방어가 aggregate마다 필요, 동시성 공유 불안전, 기존 다수 aggregate를 역행 수정해야 함 | 채택하지 않음 |
| 규약을 강제하지 않고 aggregate별 자유 선택 | 변경 범위 없음 | 현재의 불일치가 그대로 남아 handler가 aggregate마다 다른 규약을 기억해야 함 | 채택하지 않음 |
| copy-on-transition으로 통일(채택안) | 단일 규약, 원자성 구조적 보장, 동시성 안전 | 필드 추가 시 전이 메서드 생성자 동반 수정 | 채택 |

## 관련 문서

- [Pabal 도메인 모델 상세](../domain/messenger-domain-model.md)
- [ADR-0004: 도메인 모델과 Persistence/JPA 모델을 분리한다](0004-separate-domain-from-persistence-contract.md)
- [ADR-0003: Messenger는 DDD + Hexagonal + CQRS 경계를 따른다](0003-adopt-ddd-hexagonal-cqrs-boundaries.md)
