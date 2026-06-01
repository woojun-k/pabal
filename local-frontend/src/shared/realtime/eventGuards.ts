import { apiContract } from '../constants/apiContract'
import type { UUID } from '../types/api'
import type {
  MemberJoinedRealtimePayload,
  MemberLeftRealtimePayload,
  MessageDeletedRealtimePayload,
  MessageEditedRealtimePayload,
  MessageReadRealtimePayload,
  MessageSentRealtimePayload,
  RoomEventEnvelope,
  RoomEventType,
  RoomSubscriptionRevokedRealtimePayload,
  TypingEventPayload,
} from '../types/realtime'

export type MessageSentEvent = RoomEventEnvelope<MessageSentRealtimePayload> & {
  type: 'MESSAGE_SENT'
}
export type MessageEditedEvent = RoomEventEnvelope<MessageEditedRealtimePayload> & {
  type: 'MESSAGE_EDITED'
}
export type MessageDeletedEvent = RoomEventEnvelope<MessageDeletedRealtimePayload> & {
  type: 'MESSAGE_DELETED'
}
export type MessageReadEvent = RoomEventEnvelope<MessageReadRealtimePayload> & {
  type: 'MESSAGE_READ'
}
export type MemberJoinedEvent = RoomEventEnvelope<MemberJoinedRealtimePayload> & {
  type: 'MEMBER_JOINED'
}
export type MemberLeftEvent = RoomEventEnvelope<MemberLeftRealtimePayload> & {
  type: 'MEMBER_LEFT'
}

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null

const isString = (value: unknown): value is string => typeof value === 'string'

const isNumber = (value: unknown): value is number => typeof value === 'number'

const isUuidLike = (value: unknown): value is UUID => isString(value) && value.length > 0

export const isRoomEventType = (value: unknown): value is RoomEventType =>
  isString(value) && (apiContract.roomEventTypes as readonly string[]).includes(value)

export const isRoomEventEnvelope = (value: unknown): value is RoomEventEnvelope => {
  if (!isRecord(value)) {
    return false
  }

  return (
    isUuidLike(value.eventId) &&
    isNumber(value.schemaVersion) &&
    isRoomEventType(value.type) &&
    isUuidLike(value.tenantId) &&
    isUuidLike(value.chatRoomId) &&
    isNumber(value.sequence) &&
    isString(value.occurredAt) &&
    isRecord(value.payload)
  )
}

export const isMessageSentEvent = (event: RoomEventEnvelope): event is MessageSentEvent =>
  event.type === 'MESSAGE_SENT'

export const isMessageEditedEvent = (event: RoomEventEnvelope): event is MessageEditedEvent =>
  event.type === 'MESSAGE_EDITED'

export const isMessageDeletedEvent = (event: RoomEventEnvelope): event is MessageDeletedEvent =>
  event.type === 'MESSAGE_DELETED'

export const isMessageReadEvent = (event: RoomEventEnvelope): event is MessageReadEvent =>
  event.type === 'MESSAGE_READ'

export const isMemberJoinedEvent = (event: RoomEventEnvelope): event is MemberJoinedEvent =>
  event.type === 'MEMBER_JOINED'

export const isMemberLeftEvent = (event: RoomEventEnvelope): event is MemberLeftEvent =>
  event.type === 'MEMBER_LEFT'

export const isTypingEventPayload = (value: unknown): value is TypingEventPayload => {
  if (!isRecord(value)) {
    return false
  }

  return (
    isUuidLike(value.userId) &&
    isString(value.status) &&
    (apiContract.typingStatuses as readonly string[]).includes(value.status) &&
    isString(value.occurredAt)
  )
}

export const isRoomSubscriptionRevokedPayload = (
  value: unknown,
): value is RoomSubscriptionRevokedRealtimePayload => {
  if (!isRecord(value)) {
    return false
  }

  return isUuidLike(value.tenantId) && isUuidLike(value.chatRoomId) && isString(value.revokedAt)
}

export const parseRoomEvent = (body: string) => {
  const parsed = JSON.parse(body) as unknown
  if (!isRoomEventEnvelope(parsed)) {
    throw new Error('Invalid room event envelope')
  }
  return parsed
}

export const parseTypingEvent = (body: string) => {
  const parsed = JSON.parse(body) as unknown
  if (!isTypingEventPayload(parsed)) {
    throw new Error('Invalid typing event payload')
  }
  return parsed
}
