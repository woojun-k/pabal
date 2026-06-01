import { httpClient } from '../../shared/api/httpClient'
import { apiPaths } from '../../shared/api/apiPaths'
import { cursorParam, normalizePageSize } from '../../shared/utils/pagination'
import type {
  DeleteMessageResponse,
  EditMessageRequest,
  EditMessageResponse,
  MarkReadRequest,
  MessagePageResponse,
  MessageResponse,
  SendMessageRequest,
  SendMessageResponse,
  UnreadCountResponse,
  UUID,
} from '../../shared/types/api'

export const listMessages = async (chatRoomId: UUID, cursor?: number | null, size?: number) => {
  const response = await httpClient.get<MessagePageResponse>(
    apiPaths.messages(chatRoomId),
    {
      params: {
        cursor: cursorParam(cursor),
        size: normalizePageSize(size),
      },
    },
  )
  return response.data
}

export const readMessage = async (chatRoomId: UUID, messageId: UUID) => {
  const response = await httpClient.get<MessageResponse>(apiPaths.message(chatRoomId, messageId))
  return response.data
}

export const sendMessage = async (chatRoomId: UUID, request: SendMessageRequest) => {
  const response = await httpClient.post<SendMessageResponse>(
    apiPaths.messages(chatRoomId),
    request,
  )
  return response.data
}

export const sendReply = async (
  chatRoomId: UUID,
  replyToMessageId: UUID,
  request: SendMessageRequest,
) => {
  const response = await httpClient.post<SendMessageResponse>(
    apiPaths.replies(chatRoomId, replyToMessageId),
    request,
  )
  return response.data
}

export const editMessage = async (
  chatRoomId: UUID,
  messageId: UUID,
  request: EditMessageRequest,
) => {
  const response = await httpClient.patch<EditMessageResponse>(
    apiPaths.message(chatRoomId, messageId),
    request,
  )
  return response.data
}

export const deleteMessage = async (chatRoomId: UUID, messageId: UUID) => {
  const response = await httpClient.delete<DeleteMessageResponse>(
    apiPaths.message(chatRoomId, messageId),
  )
  return response.data
}

export const markRead = async (chatRoomId: UUID, request: MarkReadRequest) => {
  await httpClient.put(apiPaths.readState(chatRoomId), request)
}

export const getUnreadCount = async (chatRoomId: UUID) => {
  const response = await httpClient.get<UnreadCountResponse>(apiPaths.unreadCount(chatRoomId))
  return response.data
}
