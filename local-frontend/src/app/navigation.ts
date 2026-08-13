import { useCallback } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { useAuthStore } from '../features/auth/authStore'
import { useNotificationStore } from '../features/notifications/notificationStore'
import { useRoomStore } from '../features/rooms/roomStore'
import type { UUID } from '../shared/types/api'
import { contactsPath, roomPath, roomsPath, settingsPath } from './paths'
import type { AppTab } from './tabs'

export function useAppNavigation() {
  const navigate = useNavigate()
  const location = useLocation()
  const accessToken = useAuthStore((state) => state.accessToken)
  const activeRoomId = useRoomStore((state) => state.activeRoomId)
  const clearNotificationsForRoom = useNotificationStore((state) => state.clearNotificationsForRoom)

  const goToTab = useCallback(
    (tab: AppTab) => {
      if (!accessToken) {
        navigate(settingsPath(), { replace: true })
        return
      }

      if (tab === 'etc') {
        const destination = settingsPath()
        navigate(destination, { replace: destination === location.pathname })
        return
      }

      if (tab === 'contacts') {
        const destination = contactsPath()
        navigate(destination, { replace: destination === location.pathname })
        return
      }

      const destination = activeRoomId ? roomPath(activeRoomId) : roomsPath()
      navigate(destination, { replace: destination === location.pathname })
    },
    [accessToken, activeRoomId, location.pathname, navigate],
  )

  const goToRoom = useCallback(
    (roomId: UUID) => {
      if (!accessToken) {
        return
      }

      clearNotificationsForRoom(roomId)
      const destination = roomPath(roomId)
      navigate(destination, { replace: destination === location.pathname })
    },
    [accessToken, clearNotificationsForRoom, location.pathname, navigate],
  )

  return { goToTab, goToRoom }
}
