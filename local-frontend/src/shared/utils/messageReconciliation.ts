import { apiContract } from '../constants/apiContract'
import type { Instant, MessageResponse, UUID } from '../types/api'
import type {
  MessageDeletedRealtimePayload,
  MessageEditedRealtimePayload,
  MessageSentRealtimePayload,
} from '../types/realtime'

export type MessageDeliveryStatus = 'sending' | 'sent' | 'failed'

export type ReconciledMessage = MessageResponse & {
  deliveryStatus?: MessageDeliveryStatus
}

export const optimisticMessageId = (clientMessageId: UUID) => `optimistic-${clientMessageId}`

export const isOptimisticMessage = (message: Pick<MessageResponse, 'messageId' | 'sequence'>) =>
  message.sequence === apiContract.message.optimisticSequence ||
  message.messageId.startsWith('optimistic-')

export const createOptimisticMessage = (params: {
  chatRoomId: UUID
  senderId: UUID
  clientMessageId: UUID
  content: string
  replyToMessageId?: UUID | null
  createdAt?: Instant
}): ReconciledMessage => ({
  messageId: optimisticMessageId(params.clientMessageId),
  chatRoomId: params.chatRoomId,
  senderId: params.senderId,
  clientMessageId: params.clientMessageId,
  sequence: apiContract.message.optimisticSequence,
  content: params.content,
  status: 'ACTIVE',
  replyToMessageId: params.replyToMessageId ?? null,
  createdAt: params.createdAt ?? new Date().toISOString(),
  updatedAt: null,
  deletedAt: null,
  deliveryStatus: 'sending',
})

export const sortMessagesBySequence = <TMessage extends Pick<MessageResponse, 'sequence' | 'createdAt'>>(
  messages: TMessage[],
): TMessage[] =>
  [...messages].sort((a, b) => {
    if (a.sequence === apiContract.message.optimisticSequence && b.sequence !== apiContract.message.optimisticSequence) {
      return 1
    }
    if (b.sequence === apiContract.message.optimisticSequence && a.sequence !== apiContract.message.optimisticSequence) {
      return -1
    }
    return a.sequence - b.sequence || a.createdAt.localeCompare(b.createdAt)
  })

export const mergeMessagesByClientMessageId = <TMessage extends ReconciledMessage>(
  current: TMessage[],
  incoming: TMessage[],
): TMessage[] => {
  const byMessageId = new Map<string, TMessage>()
  const bySenderClientMessageId = new Map<string, TMessage>()

  const senderClientMessageKey = (message: Pick<MessageResponse, 'senderId' | 'clientMessageId'>) =>
    `${message.senderId}:${message.clientMessageId}`

  const upsert = (message: TMessage) => {
    const clientKey = senderClientMessageKey(message)
    const existing =
      byMessageId.get(message.messageId) ?? bySenderClientMessageId.get(clientKey)
    const merged = existing ? ({ ...existing, ...message } as TMessage) : message

    if (existing) {
      byMessageId.delete(existing.messageId)
      bySenderClientMessageId.delete(senderClientMessageKey(existing))
    }

    byMessageId.set(merged.messageId, merged)
    bySenderClientMessageId.set(senderClientMessageKey(merged), merged)
  }

  for (const message of current) {
    upsert(message)
  }

  for (const message of incoming) {
    upsert(message)
  }

  return sortMessagesBySequence([...byMessageId.values()])
}

export const mapPageMessagesToSent = (messages: MessageResponse[]): ReconciledMessage[] =>
  messages.map((message) => ({
    ...message,
    deliveryStatus: 'sent' as const,
  }))

export const messageFromSentPayload = (
  payload: MessageSentRealtimePayload,
): ReconciledMessage => ({
  messageId: payload.messageId,
  chatRoomId: payload.chatRoomId,
  senderId: payload.senderId,
  clientMessageId: payload.clientMessageId,
  sequence: payload.sequence,
  content: payload.content,
  status: 'ACTIVE',
  replyToMessageId: null,
  createdAt: payload.createdAt,
  updatedAt: null,
  deletedAt: null,
  deliveryStatus: 'sent',
})

export const applyMessageEditedPayload = (
  messages: ReconciledMessage[],
  payload: MessageEditedRealtimePayload,
): ReconciledMessage[] =>
  messages.map((message) =>
    message.messageId === payload.messageId
      ? {
          ...message,
          content: payload.content,
          status: 'EDITED' as const,
          updatedAt: payload.updatedAt,
        }
      : message,
  )

export const applyMessageDeletedPayload = (
  messages: ReconciledMessage[],
  payload: MessageDeletedRealtimePayload,
): ReconciledMessage[] =>
  messages.map((message) =>
    message.messageId === payload.messageId
      ? {
          ...message,
          status: 'DELETED' as const,
          deletedAt: payload.deletedAt,
        }
      : message,
  )

export const markMessageDeliveryFailed = (
  messages: ReconciledMessage[],
  clientMessageId: UUID,
): ReconciledMessage[] =>
  messages.map((message) =>
    message.clientMessageId === clientMessageId
      ? {
          ...message,
          deliveryStatus: 'failed' as const,
        }
      : message,
  )
