import { http, HttpResponse } from 'msw'
import { apiPaths } from '../shared/api/apiPaths'
import { apiContract } from '../shared/constants/apiContract'
import type {
  CreateChannelRoomRequest,
  CreateGroupRoomRequest,
  EditMessageRequest,
  GetOrCreateDirectRoomRequest,
  MarkReadRequest,
  SendMessageRequest,
  UUID,
} from '../shared/types/api'
import { createMockToken, principalFromAuthorization } from './mockAuth'
import type { MockPrincipal } from './mockAuth'
import { mockDb } from './mockDb'
import { mockRealtimeClient } from './realtime/mockRealtimeClient'

const noContent = () => new HttpResponse(null, { status: 204 })

const errorResponse = (status: number, code: string, message: string, path: string) =>
  HttpResponse.json(
    {
      code,
      message,
      path,
      status,
      timestamp: new Date().toISOString(),
      traceId: `mock-${crypto.randomUUID()}`,
    },
    { status },
  )

const routeParam = (value: string | readonly string[] | undefined) =>
  Array.isArray(value) ? value[0] : value

const principalOrError = (request: Request): MockPrincipal | Response => {
  const principal = principalFromAuthorization(request.headers.get('authorization'))

  return principal ?? errorResponse(401, 'MOCK401', 'Missing or invalid mock token', new URL(request.url).pathname)
}

const emitRoomEvent = (event: Parameters<typeof mockRealtimeClient.publishRoomEvent>[0] | null) => {
  if (!event) {
    return
  }

  globalThis.setTimeout(() => mockRealtimeClient.publishRoomEvent(event), 20)
}

const emitUserControl = (tenantId: UUID, chatRoomId: UUID) => {
  globalThis.setTimeout(() => {
    mockRealtimeClient.publishUserControl({
      tenantId,
      chatRoomId,
      revokedAt: new Date().toISOString(),
    })
  }, 20)
}

export const handlers = [
  http.get(apiPaths.devToken(), ({ request }) => {
    const url = new URL(request.url)
    const userId = url.searchParams.get('userId')
    const tenantId = url.searchParams.get('tenantId')
    const roles = url.searchParams.getAll('role')

    if (!userId || !tenantId) {
      return errorResponse(400, 'MOCK400', 'userId and tenantId are required', url.pathname)
    }

    return HttpResponse.json({
      accessToken: createMockToken({ userId, tenantId, roles }),
    })
  }),

  http.get(apiPaths.chatRooms(), ({ request }) => {
    const principal = principalOrError(request)

    if (principal instanceof Response) {
      return principal
    }

    return HttpResponse.json(mockDb.listRooms(principal))
  }),

  http.post(apiPaths.directRooms(), async ({ request }) => {
    const principal = principalOrError(request)

    if (principal instanceof Response) {
      return principal
    }

    const body = await request.json() as GetOrCreateDirectRoomRequest

    if (!body.participantId || body.participantId === principal.userId) {
      return errorResponse(400, 'MOCK400', 'A different participantId is required', new URL(request.url).pathname)
    }

    return HttpResponse.json(mockDb.getOrCreateDirectRoom(principal, body.participantId))
  }),

  http.post(apiPaths.groupRooms(), async ({ request }) => {
    const principal = principalOrError(request)

    if (principal instanceof Response) {
      return principal
    }

    const body = await request.json() as CreateGroupRoomRequest
    return HttpResponse.json(mockDb.createGroupRoom(principal, body))
  }),

  http.post(apiPaths.channelRooms(), async ({ request }) => {
    const principal = principalOrError(request)

    if (principal instanceof Response) {
      return principal
    }

    const body = await request.json() as CreateChannelRoomRequest
    return HttpResponse.json(mockDb.createChannelRoom(principal, body))
  }),

  http.put(apiPaths.myRoomMembership(':chatRoomId'), ({ request, params }) => {
    const principal = principalOrError(request)

    if (principal instanceof Response) {
      return principal
    }

    const chatRoomId = routeParam(params.chatRoomId)
    return chatRoomId && mockDb.joinRoom(principal, chatRoomId) ? noContent() : errorResponse(404, 'MOCK404', 'Room not found', new URL(request.url).pathname)
  }),

  http.delete(apiPaths.myRoomMembership(':chatRoomId'), ({ request, params }) => {
    const principal = principalOrError(request)

    if (principal instanceof Response) {
      return principal
    }

    const chatRoomId = routeParam(params.chatRoomId)

    if (!chatRoomId || !mockDb.leaveRoom(principal, chatRoomId)) {
      return errorResponse(404, 'MOCK404', 'Room not found', new URL(request.url).pathname)
    }

    emitUserControl(principal.tenantId, chatRoomId)
    return noContent()
  }),

  http.put(apiPaths.roomDeletionSchedule(':chatRoomId'), ({ request, params }) => {
    const principal = principalOrError(request)

    if (principal instanceof Response) {
      return principal
    }

    const chatRoomId = routeParam(params.chatRoomId)
    return chatRoomId && mockDb.scheduleRoomDeletion(principal, chatRoomId) ? noContent() : errorResponse(404, 'MOCK404', 'Room not found', new URL(request.url).pathname)
  }),

  http.delete(apiPaths.chatRoom(':chatRoomId'), ({ request, params }) => {
    const principal = principalOrError(request)

    if (principal instanceof Response) {
      return principal
    }

    const chatRoomId = routeParam(params.chatRoomId)
    return chatRoomId && mockDb.scheduleRoomDeletion(principal, chatRoomId) ? noContent() : errorResponse(404, 'MOCK404', 'Room not found', new URL(request.url).pathname)
  }),

  http.get(apiPaths.messages(':chatRoomId'), ({ request, params }) => {
    const principal = principalOrError(request)

    if (principal instanceof Response) {
      return principal
    }

    const chatRoomId = routeParam(params.chatRoomId)

    if (!chatRoomId) {
      return errorResponse(404, 'MOCK404', 'Room not found', new URL(request.url).pathname)
    }

    const url = new URL(request.url)
    const cursor = url.searchParams.get('cursor')
    const size = Number(url.searchParams.get('size') ?? apiContract.message.pageSizeDefault)

    return HttpResponse.json(
      mockDb.listMessages(
        principal,
        chatRoomId,
        cursor ? Number(cursor) : null,
        Number.isFinite(size) ? size : apiContract.message.pageSizeDefault,
      ),
    )
  }),

  http.get(apiPaths.message(':chatRoomId', ':messageId'), ({ request, params }) => {
    const principal = principalOrError(request)

    if (principal instanceof Response) {
      return principal
    }

    const chatRoomId = routeParam(params.chatRoomId)
    const messageId = routeParam(params.messageId)
    const message = chatRoomId && messageId
      ? mockDb.getMessage(principal, chatRoomId, messageId)
      : null

    return message
      ? HttpResponse.json(message)
      : errorResponse(404, 'MOCK404', 'Message not found', new URL(request.url).pathname)
  }),

  http.post(apiPaths.messages(':chatRoomId'), async ({ request, params }) => {
    const principal = principalOrError(request)

    if (principal instanceof Response) {
      return principal
    }

    const chatRoomId = routeParam(params.chatRoomId)
    const body = await request.json() as SendMessageRequest
    const result = chatRoomId ? mockDb.sendMessage(principal, chatRoomId, body) : null

    if (!result) {
      return errorResponse(404, 'MOCK404', 'Room not found', new URL(request.url).pathname)
    }

    emitRoomEvent(result.event)
    return HttpResponse.json(result.response)
  }),

  http.post(apiPaths.replies(':chatRoomId', ':replyToMessageId'), async ({ request, params }) => {
    const principal = principalOrError(request)

    if (principal instanceof Response) {
      return principal
    }

    const chatRoomId = routeParam(params.chatRoomId)
    const replyToMessageId = routeParam(params.replyToMessageId)
    const body = await request.json() as SendMessageRequest
    const result = chatRoomId && replyToMessageId
      ? mockDb.sendReply(principal, chatRoomId, replyToMessageId, body)
      : null

    if (!result) {
      return errorResponse(404, 'MOCK404', 'Room not found', new URL(request.url).pathname)
    }

    emitRoomEvent(result.event)
    return HttpResponse.json(result.response)
  }),

  http.patch(apiPaths.message(':chatRoomId', ':messageId'), async ({ request, params }) => {
    const principal = principalOrError(request)

    if (principal instanceof Response) {
      return principal
    }

    const chatRoomId = routeParam(params.chatRoomId)
    const messageId = routeParam(params.messageId)
    const body = await request.json() as EditMessageRequest
    const result = chatRoomId && messageId
      ? mockDb.editMessage(principal, chatRoomId, messageId, body.newContent)
      : null

    if (!result) {
      return errorResponse(404, 'MOCK404', 'Message not found', new URL(request.url).pathname)
    }

    emitRoomEvent(result.event)
    return HttpResponse.json(result.response)
  }),

  http.delete(apiPaths.message(':chatRoomId', ':messageId'), ({ request, params }) => {
    const principal = principalOrError(request)

    if (principal instanceof Response) {
      return principal
    }

    const chatRoomId = routeParam(params.chatRoomId)
    const messageId = routeParam(params.messageId)
    const result = chatRoomId && messageId
      ? mockDb.deleteMessage(principal, chatRoomId, messageId)
      : null

    if (!result) {
      return errorResponse(404, 'MOCK404', 'Message not found', new URL(request.url).pathname)
    }

    emitRoomEvent(result.event)
    return HttpResponse.json(result.response)
  }),

  http.put(apiPaths.readState(':chatRoomId'), async ({ request, params }) => {
    const principal = principalOrError(request)

    if (principal instanceof Response) {
      return principal
    }

    const chatRoomId = routeParam(params.chatRoomId)
    const body = await request.json() as MarkReadRequest
    const result = chatRoomId
      ? mockDb.markRead(principal, chatRoomId, body.lastReadMessageId)
      : null

    if (!result) {
      return errorResponse(404, 'MOCK404', 'Message not found', new URL(request.url).pathname)
    }

    emitRoomEvent(result.event)
    return noContent()
  }),

  http.get(apiPaths.unreadCount(':chatRoomId'), ({ request, params }) => {
    const principal = principalOrError(request)

    if (principal instanceof Response) {
      return principal
    }

    const chatRoomId = routeParam(params.chatRoomId)
    const unreadCount = chatRoomId ? mockDb.unreadCount(principal, chatRoomId) : null

    return unreadCount === null
      ? errorResponse(404, 'MOCK404', 'Room not found', new URL(request.url).pathname)
      : HttpResponse.json({ unreadCount })
  }),
]
