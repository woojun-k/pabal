import { useAuthStore } from '../../features/auth/authStore'
import { MessagePanel } from '../../features/messages/MessagePanel'
import { useRoomStore } from '../../features/rooms/roomStore'
import { useRoomRouteSync } from './useRoomRouteSync'

export function MessagesRoute() {
  useRoomRouteSync()
  const userId = useAuthStore((state) => state.userId)
  const activeRoomId = useRoomStore((state) => state.activeRoomId)

  return <MessagePanel activeRoomId={activeRoomId} currentUserId={userId} />
}
