import { apiContract } from '../constants/apiContract'
import { appPaths } from '../config/paths'
import type { UUID } from '../types/api'

const api = apiContract.apiPrefix

export const apiPaths = {
  devToken: () => appPaths.devToken,
  health: () => appPaths.health,

  chatRooms: () => `${api}/chat-rooms`,
  directRooms: () => `${api}/chat-rooms/direct`,
  groupRooms: () => `${api}/chat-rooms/groups`,
  channelRooms: () => `${api}/chat-rooms/channels`,
  chatRoom: (chatRoomId: UUID) => `${api}/chat-rooms/${chatRoomId}`,
  myRoomMembership: (chatRoomId: UUID) => `${api}/chat-rooms/${chatRoomId}/members/me`,
  roomDeletionSchedule: (chatRoomId: UUID) =>
    `${api}/chat-rooms/${chatRoomId}/deletion-schedule`,

  messages: (chatRoomId: UUID) => `${api}/chat-rooms/${chatRoomId}/messages`,
  message: (chatRoomId: UUID, messageId: UUID) =>
    `${api}/chat-rooms/${chatRoomId}/messages/${messageId}`,
  replies: (chatRoomId: UUID, replyToMessageId: UUID) =>
    `${api}/chat-rooms/${chatRoomId}/messages/${replyToMessageId}/replies`,
  readState: (chatRoomId: UUID) => `${api}/chat-rooms/${chatRoomId}/read-state`,
  unreadCount: (chatRoomId: UUID) => `${api}/chat-rooms/${chatRoomId}/unread-count`,
} as const
