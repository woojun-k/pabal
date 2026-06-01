import type { UUID } from '../types/api'

export const appDestinations = {
  typingStart: '/app/chat.typing.start',
  typingStop: '/app/chat.typing.stop',
} as const

export const subscriptionDestinations = {
  roomEvents: (tenantId: UUID, chatRoomId: UUID) =>
    `/topic/tenants/${tenantId}/chat-rooms/${chatRoomId}/events`,
  roomTyping: (tenantId: UUID, chatRoomId: UUID) =>
    `/topic/tenants/${tenantId}/chat-rooms/${chatRoomId}/typing`,
  userControl: '/user/queue/chat.control',
} as const
