import type { MessageStatus, RoomStatus, RoomType } from '../constants/apiContract'

export type UUID = string
export type Instant = string

export type LocalTokenResponse = {
  accessToken: string
}

export type RoomResponse = {
  roomId: UUID
  name: string
  type: RoomType
  status: RoomStatus
  lastMessageId: UUID | null
  lastMessageAt: Instant | null
  unreadCount: number
  joinedAt: Instant
}

export type MessageResponse = {
  messageId: UUID
  chatRoomId: UUID
  senderId: UUID
  clientMessageId: UUID
  sequence: number
  content: string
  status: MessageStatus
  replyToMessageId: UUID | null
  createdAt: Instant
  updatedAt: Instant | null
  deletedAt: Instant | null
}

export type MessagePageResponse = {
  messages: MessageResponse[]
  nextCursor: number | null
  hasNext: boolean
}

export type SendMessageRequest = {
  clientMessageId: UUID
  content: string
}

export type SendReplyRequest = SendMessageRequest

export type SendMessageResponse = {
  messageId: UUID
  sequence: number
  clientMessageId: UUID
  createdAt: Instant
  duplicated: boolean
}

export type EditMessageRequest = {
  newContent: string
}

export type EditMessageResponse = {
  messageId: UUID
  sequence: number
  content: string
  updatedAt: Instant
}

export type DeleteMessageResponse = {
  messageId: UUID
  sequence: number
  deletedAt: Instant
}

export type MarkReadRequest = {
  lastReadMessageId: UUID
}

export type UnreadCountResponse = {
  unreadCount: number
}

export type GetOrCreateDirectRoomRequest = {
  participantId: UUID
}

export type GetOrCreateDirectRoomResponse = {
  chatRoomId: UUID
}

export type CreateGroupRoomRequest = {
  participantIds: UUID[]
  roomName?: string | null
}

export type CreateChannelRoomRequest = {
  workspaceId: UUID
  channelName: string
  isPrivate: boolean
  description?: string | null
  participantIds?: UUID[] | null
}

export type CreateRoomResponse = {
  chatRoomId: UUID
  roomName: string
}
