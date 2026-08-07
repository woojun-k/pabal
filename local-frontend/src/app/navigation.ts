import { useNavigate } from 'react-router'
import { useAuthStore } from '../features/auth/authStore'
import { useNotificationStore } from '../features/notifications/notificationStore'
import { useRoomStore } from '../features/rooms/roomStore'
import type { UUID } from '../shared/types/api'
import { clientPath, contactsPath, roomPath, settingsPath } from './paths'
import type { AppTab } from './tabs'

export function useAppNavigation() {
  const navigate = useNavigate()
  const tenantId = useAuthStore((state) => state.tenantId)
  const activeRoomId = useRoomStore((state) => state.activeRoomId)
  const clearNotificationsForRoom = useNotificationStore((state) => state.clearNotificationsForRoom)

  const goToTab = (tab: AppTab) => {
    if (tab === 'etc' || !tenantId) {
      navigate(settingsPath())
      return
    }

    if (tab === 'contacts') {
      navigate(contactsPath(tenantId))
      return
    }

    navigate(activeRoomId ? roomPath(tenantId, activeRoomId) : clientPath(tenantId))
  }

  const goToRoom = (roomId: UUID) => {
    if (!tenantId) {
      return
    }

    clearNotificationsForRoom(roomId)
    navigate(roomPath(tenantId, roomId))
  }

  return { goToTab, goToRoom }
}
