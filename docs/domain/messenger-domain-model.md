---
tags:
  - pabal
  - domain
  - ddd
---

# Pabal 도메인 모델 상세

> 상위 문서: [Pabal 상세 설계 허브](../design/design-hub.md)
> 관련 문서: [Pabal 아키텍처 개요](../architecture/overview.md), [Pabal Persistence 경계와 데이터 변환](../architecture/persistence-boundary-and-mapping.md), [Pabal 데이터베이스 스키마와 제약](../architecture/database-schema-and-constraints.md), [Pabal Command-Query 유스케이스 카탈로그](../use-cases/command-query-catalog.md), [Pabal 멀티모듈 전환 전략](../architecture/multi-module-transition.md)

## 한눈에 보기

Layer: Domain
Module: `pabal-messenger-domain`, `pabal-workspace-domain`, `pabal-user-domain`
Status: Implemented

도메인 모델은 `ChatRoom`, `ChatRoomMember`, `Message`, `DirectChatMapping`을 중심으로 구성된다. Messenger에서 참조하는 workspace membership invariant는 `pabal-workspace-domain`의 `WorkspaceMember`가 소유한다. tenant user aggregate invariant는 `pabal-user-domain`의 `User`가 소유한다. repository port는 현재 domain이 아니라 application layer의 `port.out.persistence`에 있다.

## Aggregate / Entity

### ChatRoom

Layer: Domain

주요 속성:

- `id`, `type`, `name`, `createdBy`, `tenantId`
- `channelSettings`
- `status`, `scheduledDeletionAt`, `deletedAt`
- `lastMessageId`, `lastMessageSequence`, `lastMessageAt`

핵심 규칙:

- `createDirect`, `createGroup`, `createChannel` factory를 제공한다.
- `ACTIVE` 상태에서만 send/read/subscribe가 허용된다.
- self-join은 `ACTIVE` public channel에만 허용된다. direct/group/private channel은 초대 또는 별도 멤버 추가 흐름으로 다뤄야 한다.
- channel room만 deletion schedule과 immediate deletion 대상이다.
- `scheduleForDeletion`은 기본 30일 retention을 적용한다.
- `deleteImmediately`는 `PENDING_DELETION` 상태에서만 가능하며 `DELETED` 전이 시 `deletedAt`을 설정한다.
- `updateLastMessage`는 기존 `lastMessageSequence`보다 작은 sequence만 거부한다(`>` guard). 즉 같은 sequence로의 호출은 last-message snapshot pointer를 갱신하는 no-op이 아니라 최신 값으로 다시 반영된다. 이는 `Message.assignSequence`의 `>=` guard와 의도적으로 다른 경계이며(아래 Message 참고), 두 경계 모두 `ChatRoomTest`/`MessageTest`의 characterization test로 고정되어 있다.

### ChatRoomMember

Layer: Domain

주요 속성:

- `tenantId`, `chatRoomId`, `userId`
- `lastReadMessageId`, `lastReadSequence`, `lastReadAt`
- `joinedAt`, `leftAt`

핵심 규칙:

- `leftAt == null`이면 active member다.
- `updateLastRead(messageId, sequence, readAt)`은 `sequence < lastReadSequence`(stale)일 때만 원래 instance를 그대로 반환하고, `sequence == lastReadSequence`를 포함해 그 이상이면 `lastReadAt`/`lastReadMessageId`를 갱신한 새 instance를 반환한다.
- `wouldAdvanceLastReadCursorTo(sequence)`는 `sequence > lastReadSequence`일 때만 true다. `sequence == lastReadSequence`는 false이며, 이는 `MarkReadCommandHandler`가 "read cursor를 persist할지"와 "read cursor가 실제로 전진해 `MessageReadEvent`를 발행할지"를 서로 다른 질문으로 분리해서 판단하기 위한 의도적인 경계 차이다. 두 메서드는 중복이 아니라 별도 목적을 가지며, `==` 경계는 `ChatRoomMemberTest`의 characterization test로 고정되어 있다.
- inactive member는 `rejoin`으로 재활성화할 수 있다.
- active member에게 `rejoin`을 호출하면 `MemberAlreadyActiveException`이 발생한다.

### WorkspaceMember

Layer: Domain
Module: `pabal-workspace-domain`
Status: Implemented

주요 속성:

- `id`, `tenantId`, `workspaceId`, `userId`
- `role`
- `status`
- `joinedAt`, `leftAt`, `createdAt`, `updatedAt` — 10개 필드 모두 `private final`이다.

핵심 규칙:

- `WorkspaceMember`는 immutable aggregate다. 생성자 이후 필드 재할당이 없고, in-place mutator(`void` 상태 변경 메서드)가 없다.
- `joinOwner`와 `join`은 새 workspace member를 `ACTIVE` 상태로 만든다.
- `leave(Instant leftAt, boolean hasAnotherActiveOwner)`가 workspace member leave를 표현하는 business transition API이며, receiver를 바꾸지 않고 새 `WorkspaceMember` instance를 반환한다.
- `changeRole(WorkspaceRole newRole, Instant changedAt, boolean hasAnotherActiveOwner)`가 workspace member role 변경을 표현하는 business transition API이며, receiver를 바꾸지 않고 새 `WorkspaceMember` instance를 반환한다.
- `leftAt`과 `changedAt`은 caller-supplied time이다. Domain은 membership 전이 시간 계산을 위해 `Instant.now()`를 호출하지 않는다.
- active `MEMBER`와 `ADMIN`은 `hasAnotherActiveOwner` 값과 무관하게 leave할 수 있다.
- active `OWNER`는 다른 active owner가 있다는 evidence인 `hasAnotherActiveOwner=true`일 때만 leave할 수 있다.
- 마지막 active `OWNER` leave와 이미 `LEFT`인 member의 재 leave는 `WorkspaceMemberLeaveNotAllowedException`, `WSP409001`로 거부된다.
- `leftAt == null`은 상태 변경 전에 거부된다.
- 성공하면 반환된 instance가 `LEFT`가 되고 `leftAt`과 `updatedAt`은 인자로 받은 `leftAt`이 된다. receiver는 그대로 남는다.
- 성공 시 반환된 instance의 `id`, `tenantId`, `workspaceId`, `userId`, `role`, `joinedAt`, `createdAt`은 유지된다.
- role 변경은 `ACTIVE` member에게만 허용된다. 이미 `LEFT`인 member의 role 변경은 `WorkspaceMemberRoleChangeNotAllowedException`, `WSP409002`로 거부된다.
- 같은 role로의 `changeRole` 호출은 idempotent no-op이며 receiver instance를 그대로 반환하고 `updatedAt`도 바꾸지 않는다.
- `MEMBER`/`ADMIN`에서 `OWNER`로의 승격, `MEMBER`와 `ADMIN` 사이의 변경은 다른 active owner evidence 없이 허용된다.
- active `OWNER`를 `ADMIN` 또는 `MEMBER`로 내리는 변경은 다른 active owner가 있다는 evidence인 `hasAnotherActiveOwner=true`일 때만 허용된다. 마지막 active `OWNER` demotion은 `WSP409002`로 거부된다.
- role 변경 성공 시 반환된 instance의 `role`과 `updatedAt`만 바뀌며 identity, status, join/create/left 값은 유지된다.
- 거부된 leave와 role 변경은 새 instance를 만들지 않고 receiver snapshot을 부분 변경하지도 않는다.
- `snapshot()`은 `LEFT` 상태와 non-null `leftAt` 정합성을 보존한다. `reconstitute(WorkspaceMemberSnapshot)`는 persistence hydration 용도이며 leave business transition API가 아니다.

### User

Layer: Domain
Module: `pabal-user-domain`
Status: Implemented

주요 속성:

- `id`, `tenantId`, `name`, `status`, `createdAt`, `updatedAt` — 6개 필드 모두 `private final`이다.

핵심 규칙:

- `User`는 immutable aggregate다. 생성자 이후 필드 재할당이 없고, in-place mutator(`void` 상태 변경 메서드)가 없다.
- `rename(String newName, Instant updatedAt)`과 `disable(Instant updatedAt)`은 receiver를 바꾸지 않고 새 `User` instance를 반환한다. 나머지 필드는 receiver 값을 그대로 복사한다.
- `rename`이 성공하면 반환된 instance의 `name`이 새 이름, `updatedAt`이 인자로 받은 값이 된다. `id`, `tenantId`, `status`, `createdAt`은 유지된다.
- `disable`이 성공하면 반환된 instance의 `status`가 `UserStatus.DISABLED`, `updatedAt`이 인자로 받은 값이 된다. `id`, `tenantId`, `name`, `createdAt`은 유지된다.
- 이미 `DISABLED`인 user에 `disable`을 호출하면 `UserAlreadyDisabledException`(`USR409002`)이 발생한다. `DISABLED` user에 `rename`을 호출해도 같은 예외가 발생한다. 두 경우 모두 새 instance는 만들어지지 않고 receiver는 그대로 남는다.
- `updatedAt`은 caller-supplied time이다. domain은 `rename`/`disable` 내부에서 `Instant.now()`를 호출하지 않는다.
- `rename`/`disable`은 `updatedAt`이 `null`이면 `Objects.requireNonNull`을 통해 `NullPointerException`을 던진다. guard 순서(null 체크와 상태 체크 사이)는 계약상 고정되어 있지 않다.
- `create`, `reconstitute`, `snapshot`, `isActive`, equality/hashCode(`id` 기준) semantic은 이번 변경으로 바뀌지 않았다.
- 현재 production caller는 없다. `rename`/`disable` 호출은 `pabal-user-domain`과 `pabal-user-infrastructure`의 regression test에서만 이뤄진다.

### Message

Layer: Domain

주요 속성:

- `tenantId`, `chatRoomId`, `senderId`, `clientMessageId`
- `sequence`, `type`, `content`, `status`, `replyToMessageId`
- `createdAt`, `updatedAt`, `deletedAt`

핵심 규칙:

- `create`와 `createReply`는 `MessageType.USER`, `MessageStatus.ACTIVE`로 메시지를 만든다.
- `assignSequence`는 이미 `sequence`가 있고 그 값이 인자 이상(`>=`)이면 원래 instance를 그대로 반환하는 no-op이다. 한 번 정해진 sequence는 불변이므로 같은 값 재할당도 no-op으로 처리한다. `ChatRoom.updateLastMessage`의 `>` guard(같은 sequence는 pointer를 갱신)와는 의도적으로 다른 경계다.
- `edit`은 삭제된 메시지에 허용되지 않는다.
- `delete`는 이미 삭제된 메시지에 허용되지 않는다.
- `snapshot()`은 persistence contract 변환의 입력으로 사용된다.

### DirectChatMapping

Layer: Domain

주요 속성:

- `tenantId`, `chatRoomId`, `userIdMin`, `userIdMax`

핵심 규칙:

- direct chat participant는 서로 다른 사용자여야 한다.
- UUID 정렬로 `userIdMin/userIdMax`를 저장해 A-B와 B-A를 같은 pair로 취급한다.

## Value Object

### RoomName 계열

- `RoomName`: room type별 name factory를 제공한다.
- `OptionalName`: direct/group room name, null 허용, 최대 50자.
- `ChannelName`: channel room name, 필수, 1~50자, 한글/영문/숫자/underscore/hyphen만 허용, 소문자 정규화.

### MessageContent

- null/blank를 허용하지 않는다.
- 최대 5000자를 허용한다.
- 메시지 삭제 시 원문은 tombstone 값 `[deleted]`로 대체한다.
- 현재 Flyway schema는 `message.content TEXT`와 `chk_message_content_length`로 1~5000자 정책을 함께 검증한다. 상세 내용은 [Pabal 데이터베이스 스키마와 제약](../architecture/database-schema-and-constraints.md)에서 관리한다.

### ChannelSettings

- `workspaceId`, `isPrivate`, `description`을 가진다.
- `withPrivacy`, `withDescription`으로 immutable style 변경을 제공한다.
- Messenger domain은 `workspaceId`를 identity 값으로 보관하지만 workspace membership을 직접 조회하지 않는다.
- channel participant 검증은 application `RoomParticipantPolicy`와 infrastructure `ContractRoomParticipantDirectoryAdapter`가 `WorkspaceContract`를 통해 수행한다.

## Enum과 상태 모델

- `RoomType`: `DIRECT`, `GROUP`, `CHANNEL`
- `RoomStatus`: `ACTIVE`, `PENDING_DELETION`, `DELETED`
- `MessageType`: `USER`, `SYSTEM`
- `MessageStatus`: `ACTIVE`, `DELETED`, `EDITED`
- `TypingStatus`: `STARTED`, `STOPPED`
- `RoomAccessOperation`: send/read/subscribe/join operation 표현

## Domain Policy

### RoomMembershipPolicy

Layer: Domain

- `canSelfJoin`은 `ACTIVE` public channel만 true를 반환한다.
- private channel, direct room, group room은 roomId를 알아도 direct join을 허용하지 않는다.
- room 상태가 active가 아니면 `RoomOperationNotAllowedException`, type/privacy 조건이 맞지 않으면 `RoomJoinForbiddenException`을 던진다.

### RoomNameFormatter

Layer: Domain

- group room 이름이 주어지지 않으면 requester/participant UUID 기반 이름을 생성한다.
- Messenger domain은 사용자 profile/name을 직접 조회하지 않고 deterministic fallback name을 만든다.
- display name 연동이 필요하면 user module repository 직접 의존이 아니라 application service 또는 `UserContract` 기반 경계를 통해 처리한다.

## 도메인 이벤트

Layer: Domain

- `MessageSentEvent`
- `MessageEditedEvent`
- `MessageDeletedEvent`
- `MessageReadEvent`
- `MemberJoinedEvent`
- `MemberLeftEvent`

이 이벤트들은 `DomainEvent`를 구현하고 application handler에서 `DomainEventPublisher.publishAfterCommit`으로 발행된다. realtime payload 변환은 application listener와 contract realtime 모델이 담당한다.

## Repository Port 위치

Status: Implemented

현재 코드에서 repository port는 domain이 아니라 application layer에 있다.

```text
pabal-messenger-application
└─ src/main/java/com/polarishb/pabal/messenger/application/port/out/persistence
```

주요 port:

- `MessageRepository`, `MessageReadRepository`, `MessageWriteRepository`
- `ChatRoomRepository`, `ChatRoomReadRepository`, `ChatRoomWriteRepository`
- `ChatRoomMemberRepository`, `ChatRoomMemberReadRepository`, `ChatRoomMemberWriteRepository`
- `DirectChatMappingRepository`, `DirectChatMappingReadRepository`, `DirectChatMappingWriteRepository`
- `ChatRoomSequenceRepository`

의미:

- domain은 persistence 저장소 인터페이스도 직접 소유하지 않는다.
- application이 use case 관점의 outbound port를 정의한다.
- infrastructure가 그 port를 구현한다.

## 도메인 모델을 수정할 때 체크할 것

- 변경이 domain invariant인지 application orchestration인지 먼저 구분한다.
- aggregate 필드는 `private final`로 선언하고, 상태 전이는 receiver를 바꾸지 않고 새 instance를 반환한다. `void` in-place mutator는 두지 않는다. 모든 domain aggregate(`ChatRoom`, `ChatRoomMember`, `Message`, `DirectChatMapping`, `WorkspaceMember`, `Workspace`, `User`, `Tenant`, `TenantRegistration`)가 이 규약을 따른다. 근거는 [ADR-0014](../adr/0014-domain-aggregates-are-immutable-with-copy-on-transition.md).
- `tenantId`는 모든 aggregate에 전파되어야 한다.
- domain에 `State`, `Persisted*`, JPA Entity import가 들어오면 안 된다.
- 상태 전이에 필요한 예외는 `MessengerErrorCode`와 함께 public error mapping을 확인한다.
- realtime event가 필요한 상태 변경이라면 application handler/listener 흐름까지 같이 본다.
