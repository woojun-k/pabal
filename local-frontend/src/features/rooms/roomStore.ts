import { useMemo } from 'react'
import { create } from 'zustand'
import { toApiError, type ApiError } from '../../shared/api/apiError'
import { isMessageReadEvent, isMessageSentEvent } from '../../shared/realtime/eventGuards'
import type {
  CreateChannelRoomRequest,
  CreateGroupRoomRequest,
  GetOrCreateDirectRoomRequest,
  RoomResponse,
  UUID,
} from '../../shared/types/api'
import type { RoomEventEnvelope } from '../../shared/types/realtime'
import {
  createChannelRoom,
  createGroupRoom,
  deleteRoomImmediately,
  getOrCreateDirectRoom,
  leaveRoom,
  listRooms,
  markRoomRead,
} from './roomsApi'

type RoomState = {
  rooms: RoomResponse[]
  activeRoomId: UUID | null
  hasLoadedRooms: boolean
  isLoading: boolean
  isMutating: boolean
  error: ApiError | null
  loadRooms: () => Promise<void>
  selectRoom: (roomId: UUID | null) => Promise<void>
  resetRooms: () => void
  getActiveRoom: () => RoomResponse | null
  createDirectRoom: (request: GetOrCreateDirectRoomRequest) => Promise<UUID | null>
  createGroupRoom: (request: CreateGroupRoomRequest) => Promise<UUID | null>
  createChannelRoom: (request: CreateChannelRoomRequest) => Promise<UUID | null>
  leaveActiveRoom: () => Promise<void>
  deleteActiveRoom: () => Promise<void>
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
  hasLoadedRooms: false,
  isLoading: false,
  isMutating: false,
  error: null,

  loadRooms: async () => {
    set({ isLoading: true, error: null })

    try {
      const rooms = await listRooms()
      set({
        rooms: sortRooms(rooms),
        hasLoadedRooms: true,
        isLoading: false,
      })
    } catch (error) {
      set({ isLoading: false, error: toApiError(error) })
    }
  },

  selectRoom: async (roomId) => {
    set({ activeRoomId: roomId, error: null })

    if (!roomId) {
      return
    }

    const room = get().rooms.find((candidate) => candidate.roomId === roomId)

    if (!room?.lastMessageId || room.unreadCount === 0) {
      return
    }

    const lastReadMessageId = room.lastMessageId

    try {
      await markRoomRead(roomId, { lastReadMessageId })
      set((state) => ({
        rooms: state.rooms.map((candidate) =>
          candidate.roomId === roomId && candidate.lastMessageId === lastReadMessageId
            ? { ...candidate, unreadCount: 0 }
            : candidate,
        ),
      }))
    } catch (error) {
      set({ error: toApiError(error) })
    }
  },

  resetRooms: () => {
    set({ rooms: [], activeRoomId: null, hasLoadedRooms: false, error: null })
  },

  getActiveRoom: () => {
    const { rooms, activeRoomId } = get()
    return rooms.find((room) => room.roomId === activeRoomId) ?? null
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

  createGroupRoom: async (request) => {
    set({ isMutating: true, error: null })

    try {
      const response = await createGroupRoom(request)
      await get().loadRooms()
      set({ isMutating: false })
      return response.chatRoomId
    } catch (error) {
      set({ isMutating: false, error: toApiError(error) })
      return null
    }
  },

  createChannelRoom: async (request) => {
    set({ isMutating: true, error: null })

    try {
      const response = await createChannelRoom(request)
      await get().loadRooms()
      set({ isMutating: false })
      return response.chatRoomId
    } catch (error) {
      set({ isMutating: false, error: toApiError(error) })
      return null
    }
  },

  leaveActiveRoom: async () => {
    const roomId = get().activeRoomId

    if (!roomId) {
      return
    }

    set({ isMutating: true, error: null })

    try {
      await leaveRoom(roomId)
      set((state) => {
        const rooms = state.rooms.filter((room) => room.roomId !== roomId)
        return {
          rooms,
          isMutating: false,
        }
      })
    } catch (error) {
      set({ isMutating: false, error: toApiError(error) })
    }
  },

  deleteActiveRoom: async () => {
    const roomId = get().activeRoomId

    if (!roomId) {
      return
    }

    set({ isMutating: true, error: null })

    try {
      await deleteRoomImmediately(roomId)
      set((state) => {
        const rooms = state.rooms.filter((room) => room.roomId !== roomId)
        return {
          rooms,
          isMutating: false,
        }
      })
    } catch (error) {
      set({ isMutating: false, error: toApiError(error) })
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
