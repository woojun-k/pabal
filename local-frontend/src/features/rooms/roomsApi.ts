import { httpClient } from '../../shared/api/httpClient'
import { apiPaths } from '../../shared/api/apiPaths'
import type {
  CreateChannelRoomRequest,
  CreateGroupRoomRequest,
  CreateRoomResponse,
  GetOrCreateDirectRoomRequest,
  GetOrCreateDirectRoomResponse,
  MarkReadRequest,
  RoomResponse,
  UUID,
} from '../../shared/types/api'

export const listRooms = async () => {
  const response = await httpClient.get<RoomResponse[]>(apiPaths.chatRooms())
  return response.data
}

export const getOrCreateDirectRoom = async (request: GetOrCreateDirectRoomRequest) => {
  const response = await httpClient.post<GetOrCreateDirectRoomResponse>(
    apiPaths.directRooms(),
    request,
  )
  return response.data
}

export const createGroupRoom = async (request: CreateGroupRoomRequest) => {
  const response = await httpClient.post<CreateRoomResponse>(apiPaths.groupRooms(), request)
  return response.data
}

export const createChannelRoom = async (request: CreateChannelRoomRequest) => {
  const response = await httpClient.post<CreateRoomResponse>(
    apiPaths.channelRooms(),
    request,
  )
  return response.data
}

export const joinRoom = async (chatRoomId: UUID) => {
  await httpClient.put(apiPaths.myRoomMembership(chatRoomId))
}

export const leaveRoom = async (chatRoomId: UUID) => {
  await httpClient.delete(apiPaths.myRoomMembership(chatRoomId))
}

export const markRoomRead = async (chatRoomId: UUID, request: MarkReadRequest) => {
  await httpClient.put(apiPaths.readState(chatRoomId), request)
}

export const scheduleRoomDeletion = async (chatRoomId: UUID) => {
  await httpClient.put(apiPaths.roomDeletionSchedule(chatRoomId))
}

export const deleteRoomImmediately = async (chatRoomId: UUID) => {
  await httpClient.delete(apiPaths.chatRoom(chatRoomId))
}
