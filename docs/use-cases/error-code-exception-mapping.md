---
tags:
  - pabal
  - error
  - exception
---

# Pabal 에러 코드와 예외 매핑표

> 상위 문서: [Pabal 상세 설계 허브](../design/design-hub.md)
> 관련 문서: [Pabal HTTP API 예시와 오류 매핑](http-api-and-error-mapping.md), [Pabal Command-Query 유스케이스 카탈로그](command-query-catalog.md), [Pabal 테스트 전략](../testing/testing-strategy.md)

## 공통 에러

Layer: Common

| Code | Name | HTTP | 대표 원인 |
| --- | --- | --- | --- |
| `CMN001` | `INTERNAL_SERVER_ERROR` | 500 | 공통 내부 오류 |
| `CMN002` | `INVALID_INPUT` | 400 | validation 실패, `InvalidInputException` |
| `CMN003` | `UNAUTHORIZED` | 401 | 인증 필요 |
| `CMN004` | `FORBIDDEN` | 403 | `AccessDeniedException` |
| `CMN005` | `NOT_FOUND` | 404 | 공통 리소스 없음 |
| `CMN500001` | internal normalized | 500 | 예상하지 못한 exception |

`GlobalExceptionHandler`는 validation detail을 `ApiErrorDetail(field, reason)`으로 변환한다.

## Messenger 도메인 에러

Layer: Domain / Application

| Code | Name | HTTP | 대표 예외 |
| --- | --- | --- | --- |
| `MSG400001` | `INVALID_REPLY_TARGET` | 400 | `InvalidReplyTargetException` |
| `MSG400002` | `ROOM_CANNOT_BE_DELETED` | 400 | `RoomCannotBeDeletedException` |
| `MSG400003` | `INVALID_ROOM_STATUS` | 400 | `RoomMustBePendingDeletionException` |
| `MSG400004` | `INVALID_ROOM_STATUS_TRANSITION` | 400 | `InvalidRoomStatusTransitionException` |
| `MSG400005` | `MESSAGE_ALREADY_DELETED` | 400 | `MessageAlreadyDeletedException` |
| `MSG400006` | `INVALID_DIRECT_CHAT_PARTICIPANTS` | 400 | `InvalidDirectChatParticipantsException` |
| `MSG404001` | `CHAT_ROOM_NOT_FOUND` | 404 | `ChatRoomNotFoundException` |
| `MSG404002` | `MESSAGE_NOT_FOUND` | 404 | `MessageNotFoundException` |
| `MSG404003` | `MEMBER_NOT_FOUND` | 404 | `MemberNotFoundException` |
| `MSG404004` | `DIRECT_CHAT_MAPPING_NOT_FOUND` | 404 | `DirectChatMappingNotFoundException` |
| `MSG403001` | `MEMBER_NOT_IN_ROOM` | 403 | `MemberNotInRoomException` |
| `MSG403002` | `MEMBER_NOT_ACTIVE` | 403 | `MemberNotActiveException` |
| `MSG403003` | `MESSAGE_EDIT_FORBIDDEN` | 403 | `MessageEditForbiddenException` |
| `MSG403004` | `ROOM_DELETE_FORBIDDEN` | 403 | `UnauthorizedRoomDeletionException` |
| `MSG403005` | `MESSAGE_DELETE_FORBIDDEN` | 403 | `MessageDeleteForbiddenException` |
| `MSG403006` | `ROOM_OPERATION_NOT_ALLOWED` | 403 | `RoomOperationNotAllowedException` |
| `MSG403007` | `ROOM_JOIN_FORBIDDEN` | 403 | `RoomJoinForbiddenException` |
| `MSG403008` | `CHANNEL_PERMISSION_DENIED` | 403 | `ChannelPermissionDeniedException` |
| `MSG409001` | `DUPLICATE_MESSAGE` | 409 | `DuplicateMessageException` |
| `MSG409002` | `DUPLICATE_DIRECT_MAPPING` | 409 | `DuplicateDirectChatMappingException` |
| `MSG409003` | `DUPLICATE_CHANNEL_NAME` | 409 | `DuplicateChannelNameException` |
| `MSG409004` | `MEMBER_ALREADY_ACTIVE` | 409 | `MemberAlreadyActiveException` |

## 기술 예외 정규화

| Exception | HTTP | Code | 설명 |
| --- | --- | --- | --- |
| `AccessDeniedException` | 403 | `CMN004` | HTTP principal 누락, STOMP tenant mismatch 등 |
| `ObjectOptimisticLockingFailureException` | 409 | `CMN` conflict descriptor | version mismatch |
| `DataIntegrityViolationException` | 409 | `CMN` conflict descriptor | DB unique/check 제약 실패 |
| `MethodArgumentNotValidException` | 400 | `CMN002` | request body validation 실패 |
| `HandlerMethodValidationException` | 400 | `CMN002` | path/query parameter validation 실패 |
| `ConstraintViolationException` | 400 | `CMN002` | constraint violation |

## 구현상 포인트

- `DuplicateMessageException`은 `MessageWriteRepositoryImpl`에서 `uq_message_client_id` 위반을 감지해 던진다.
- 정상적인 중복 재시도는 handler에서 기존 메시지를 조회해 `duplicated=true`로 응답한다.
- `DuplicateDirectChatMappingException`은 direct room concurrent create race를 흡수하기 위한 재조회 흐름에서 사용된다.
- domain exception은 public payload를 가질 수 있으며, `GlobalExceptionHandler` 로그에는 내부 payload가 남는다.
