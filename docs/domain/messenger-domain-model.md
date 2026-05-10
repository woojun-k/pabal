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
Module: `pabal-messenger-domain`
Status: Implemented

도메인 모델은 `ChatRoom`, `ChatRoomMember`, `Message`, `DirectChatMapping`을 중심으로 구성된다. repository port는 현재 domain이 아니라 `pabal-messenger-application`의 `port.out.persistence`에 있다.

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

### ChatRoomMember

Layer: Domain

주요 속성:

- `tenantId`, `chatRoomId`, `userId`
- `lastReadMessageId`, `lastReadSequence`, `lastReadAt`
- `joinedAt`, `leftAt`

핵심 규칙:

- `leftAt == null`이면 active member다.
- last-read cursor는 stale sequence로 후퇴하지 않는다.
- inactive member는 `rejoin`으로 재활성화할 수 있다.
- active member에게 `rejoin`을 호출하면 `MemberAlreadyActiveException`이 발생한다.

### Message

Layer: Domain

주요 속성:

- `tenantId`, `chatRoomId`, `senderId`, `clientMessageId`
- `sequence`, `type`, `content`, `status`, `replyToMessageId`
- `createdAt`, `updatedAt`, `deletedAt`

핵심 규칙:

- `create`와 `createReply`는 `MessageType.USER`, `MessageStatus.ACTIVE`로 메시지를 만든다.
- `assignSequence`는 더 큰 sequence만 반영한다.
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
- 현재 Flyway schema는 `message.content TEXT`와 `chk_message_content_length`로 1~5000자 정책을 함께 검증한다. 상세 내용은 [Pabal 데이터베이스 스키마와 제약](../architecture/database-schema-and-constraints.md)에서 관리한다.

### ChannelSettings

- `workspaceId`, `isPrivate`, `description`을 가진다.
- `withPrivacy`, `withDescription`으로 immutable style 변경을 제공한다.

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
- 사용자 profile 조회 없이 deterministic fallback name을 만든다.
- 향후 사용자 display name 연동이 들어오면 domain policy 또는 application service 경계를 다시 검토한다.

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
- `tenantId`는 모든 aggregate에 전파되어야 한다.
- domain에 `State`, `Persisted*`, JPA Entity import가 들어오면 안 된다.
- 상태 전이에 필요한 예외는 `MessengerErrorCode`와 함께 public error mapping을 확인한다.
- realtime event가 필요한 상태 변경이라면 application handler/listener 흐름까지 같이 본다.
