import { create } from 'zustand'
import type { UUID } from '../../shared/types/api'

export type NotificationKind = 'info' | 'success' | 'warning' | 'error'

export type AppNotification = {
  id: string
  kind: NotificationKind
  title: string
  message?: string
  roomId?: UUID
  createdAt: number
}

type NotificationState = {
  notifications: AppNotification[]
  addNotification: (notification: Omit<AppNotification, 'id' | 'createdAt'>) => string
  removeNotification: (id: string) => void
  clearNotificationsForRoom: (roomId: UUID) => void
  clearNotifications: () => void
}

export const useNotificationStore = create<NotificationState>((set) => ({
  notifications: [],

  addNotification: (notification) => {
    const id = crypto.randomUUID()
    set((state) => ({
      notifications: [
        {
          ...notification,
          id,
          createdAt: Date.now(),
        },
        ...state.notifications,
      ].slice(0, 8),
    }))
    return id
  },

  removeNotification: (id) => {
    set((state) => ({
      notifications: state.notifications.filter((notification) => notification.id !== id),
    }))
  },

  clearNotificationsForRoom: (roomId) => {
    set((state) => ({
      notifications: state.notifications.filter((notification) => notification.roomId !== roomId),
    }))
  },

  clearNotifications: () => {
    set({ notifications: [] })
  },
}))
