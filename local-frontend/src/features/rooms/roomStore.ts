import { useMemo } from 'react'
import { create } from 'zustand'
import { toApiError, type ApiError } from '../../shared/api/apiError'
import { isMessageReadEvent, isMessageSentEvent } from '../../shared/realtime/eventGuards'
import type {
  GetOrCreateDirectRoomRequest,
  RoomResponse,
  UUID,
} from '../../shared/types/api'
import type { RoomEventEnvelope } from '../../shared/types/realtime'
import { tokenStorage } from '../../shared/security/tokenStorage'
import { getOrCreateDirectRoom, listRooms, markRoomRead } from './roomsApi'

/* 비동기 응답 적용 가드 — 더 새 요청이 시작됐거나(세대) 세션 토큰이 바뀐 뒤
   도착한 응답은 버린다. 세션 전환·연속 새로고침에서 stale 응답이 최신 상태를
   덮어쓰는 것을 방지 (요청 헤더는 전송 시점 토큰으로 굳으므로 store가 걸러야 함) */
let loadGeneration = 0
const sessionToken = () => tokenStorage.load()?.accessToken ?? null

/* loadStatus는 "목록을 신뢰할 수 있는가"의 latch — 'ready' 도달 후에는 새로고침이
   실패해도 'ready'를 유지한다(stale-while-revalidate). 요청 in-flight 여부는
   isFetching이 별도로 담당. 'loading'은 신뢰할 목록이 아직 없는 상태(초기·리셋 직후) */
export type RoomLoadStatus = 'loading' | 'ready' | 'error'

type RoomState = {
  rooms: RoomResponse[]
  activeRoomId: UUID | null
  loadStatus: RoomLoadStatus
  isFetching: boolean
  isMutating: boolean
  error: ApiError | null
  loadRooms: () => Promise<void>
  selectRoom: (roomId: UUID | null) => Promise<void>
  resetRooms: () => void
  createDirectRoom: (request: GetOrCreateDirectRoomRequest) => Promise<UUID | null>
  applyRoomEvent: (event: RoomEventEnvelope, currentUserId: UUID | null) => void
}

const sortRooms = (rooms: RoomResponse[]) =>
  [...rooms].sort((a, b) => {
    const aTime = a.lastMessageAt ?? a.joinedAt
    const bTime = b.lastMessageAt ?? b.joinedAt
    return bTime.localeCompare(aTime)
  })

export const useRoomStore = create<RoomState>((set, get) => ({
  rooms: [],
  activeRoomId: null,
  loadStatus: 'loading',
  isFetching: false,
  isMutating: false,
  error: null,

  loadRooms: async () => {
    const generation = ++loadGeneration
    const token = sessionToken()
    const keepReady = get().loadStatus === 'ready'
    set(
      keepReady
        ? { isFetching: true, error: null }
        : { loadStatus: 'loading', isFetching: true, error: null },
    )

    try {
      const rooms = await listRooms()

      if (generation !== loadGeneration || token !== sessionToken()) {
        return
      }

      set({
        rooms: sortRooms(rooms),
        loadStatus: 'ready',
        isFetching: false,
      })
    } catch (error) {
      if (generation !== loadGeneration || token !== sessionToken()) {
        return
      }

      set(
        keepReady
          ? { isFetching: false, error: toApiError(error) }
          : { loadStatus: 'error', isFetching: false, error: toApiError(error) },
      )
    }
  },

  selectRoom: async (roomId) => {
    if (get().activeRoomId === roomId) {
      return
    }

    set({ activeRoomId: roomId })

    if (!roomId) {
      return
    }

    const room = get().rooms.find((candidate) => candidate.roomId === roomId)

    if (!room?.lastMessageId || room.unreadCount === 0) {
      return
    }

    const lastReadMessageId = room.lastMessageId
    const token = sessionToken()

    try {
      await markRoomRead(roomId, { lastReadMessageId })

      if (token !== sessionToken()) {
        return
      }

      set((state) => ({
        rooms: state.rooms.map((candidate) =>
          candidate.roomId === roomId && candidate.lastMessageId === lastReadMessageId
            ? { ...candidate, unreadCount: 0 }
            : candidate,
        ),
      }))
    } catch (error) {
      if (token !== sessionToken()) {
        return
      }

      set({ error: toApiError(error) })
    }
  },

  resetRooms: () => {
    set({ rooms: [], activeRoomId: null, loadStatus: 'loading', isFetching: false, error: null })
  },

  createDirectRoom: async (request) => {
    set({ isMutating: true, error: null })

    try {
      const response = await getOrCreateDirectRoom(request)
      await get().loadRooms()
      set({ isMutating: false })
      return response.chatRoomId
    } catch (error) {
      set({ isMutating: false, error: toApiError(error) })
      return null
    }
  },

  applyRoomEvent: (event, currentUserId) => {
    set((state) => {
      const rooms = state.rooms.map((room) => {
        if (room.roomId !== event.chatRoomId) {
          return room
        }

        if (isMessageSentEvent(event)) {
          const payload = event.payload
          const shouldCountUnread =
            payload.senderId !== currentUserId && state.activeRoomId !== event.chatRoomId

          return {
            ...room,
            lastMessageId: payload.messageId,
            lastMessageAt: payload.createdAt,
            unreadCount: shouldCountUnread ? room.unreadCount + 1 : room.unreadCount,
          }
        }

        if (isMessageReadEvent(event) && event.payload.userId === currentUserId) {
          return {
            ...room,
            unreadCount: 0,
          }
        }

        return room
      })

      return { rooms: sortRooms(rooms) }
    })
  },
}))

const isDirectSectionRoom = (room: RoomResponse) =>
  room.type === 'DIRECT' || room.type === 'GROUP'

export function useDirectRooms(): RoomResponse[] {
  const rooms = useRoomStore((state) => state.rooms)
  return useMemo(() => rooms.filter(isDirectSectionRoom), [rooms])
}
