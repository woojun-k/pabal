import { useCallback } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { useAuthStore } from '../features/auth/authStore'
import { useNotificationStore } from '../features/notifications/notificationStore'
import { useRoomStore } from '../features/rooms/roomStore'
import type { UUID } from '../shared/types/api'
import { clientPath, contactsPath, roomPath, settingsPath } from './paths'
import type { AppTab } from './tabs'

export function useAppNavigation() {
  const navigate = useNavigate()
  const location = useLocation()
  const tenantId = useAuthStore((state) => state.tenantId)
  const activeRoomId = useRoomStore((state) => state.activeRoomId)
  const clearNotificationsForRoom = useNotificationStore((state) => state.clearNotificationsForRoom)

  const goToTab = useCallback(
    (tab: AppTab) => {
      if (!tenantId) {
        navigate(settingsPath(), { replace: true })
        return
      }

      if (tab === 'etc') {
        const destination = settingsPath()
        navigate(destination, { replace: destination === location.pathname })
        return
      }

      if (tab === 'contacts') {
        const destination = contactsPath(tenantId)
        navigate(destination, { replace: destination === location.pathname })
        return
      }

      const destination = activeRoomId ? roomPath(tenantId, activeRoomId) : clientPath(tenantId)
      navigate(destination, { replace: destination === location.pathname })
    },
    [activeRoomId, location.pathname, navigate, tenantId],
  )

  const goToRoom = useCallback(
    (roomId: UUID) => {
      if (!tenantId) {
        return
      }

      clearNotificationsForRoom(roomId)
      const destination = roomPath(tenantId, roomId)
      navigate(destination, { replace: destination === location.pathname })
    },
    [clearNotificationsForRoom, location.pathname, navigate, tenantId],
  )

  return { goToTab, goToRoom }
}
