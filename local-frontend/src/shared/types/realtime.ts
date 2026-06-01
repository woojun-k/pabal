import type { Instant, UUID } from './api'

export type TypingRequest = {
  tenantId: UUID
  chatRoomId: UUID
}

export type TypingStatus = 'STARTED' | 'STOPPED'

export type TypingEventPayload = {
  userId: UUID
  status: TypingStatus
  occurredAt: Instant
}

export type RoomEventType =
  | 'MESSAGE_SENT'
  | 'MESSAGE_EDITED'
  | 'MESSAGE_DELETED'
  | 'MESSAGE_READ'
  | 'MEMBER_JOINED'
  | 'MEMBER_LEFT'

export type MessageSentRealtimePayload = {
  messageId: UUID
  chatRoomId: UUID
  sequence: number
  senderId: UUID
  clientMessageId: UUID
  content: string
  createdAt: Instant
}

export type MessageEditedRealtimePayload = {
  messageId: UUID
  chatRoomId: UUID
  sequence: number
  content: string
  updatedAt: Instant
}

export type MessageDeletedRealtimePayload = {
  messageId: UUID
  chatRoomId: UUID
  sequence: number
  deletedAt: Instant
}

export type MessageReadRealtimePayload = {
  userId: UUID
  chatRoomId: UUID
  lastReadMessageId: UUID
  sequence: number
  readAt: Instant
}

export type MemberJoinedRealtimePayload = {
  userId: UUID
  chatRoomId: UUID
  sequence: number
  joinedAt: Instant
}

export type MemberLeftRealtimePayload = {
  userId: UUID
  chatRoomId: UUID
  sequence: number
  leftAt: Instant
}

export type RoomEventPayload =
  | MessageSentRealtimePayload
  | MessageEditedRealtimePayload
  | MessageDeletedRealtimePayload
  | MessageReadRealtimePayload
  | MemberJoinedRealtimePayload
  | MemberLeftRealtimePayload

export type RoomEventEnvelope<TPayload extends RoomEventPayload = RoomEventPayload> = {
  eventId: UUID
  schemaVersion: number
  type: RoomEventType
  tenantId: UUID
  chatRoomId: UUID
  sequence: number
  aggregateVersion: number | null
  occurredAt: Instant
  payload: TPayload
}

export type RoomSubscriptionRevokedRealtimePayload = {
  tenantId: UUID
  chatRoomId: UUID
  revokedAt: Instant
}
