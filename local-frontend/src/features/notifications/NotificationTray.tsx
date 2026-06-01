import { useNotificationStore } from './notificationStore'

export function NotificationTray() {
  const { notifications, removeNotification, clearNotifications } = useNotificationStore()

  if (notifications.length === 0) {
    return null
  }

  return (
    <aside className="notification-tray" aria-label="Notifications">
      <div className="notification-tray-header">
        <span>Notifications</span>
        <button type="button" className="ghost-button" onClick={clearNotifications}>
          Clear
        </button>
      </div>
      {notifications.map((notification) => (
        <article className={`notification is-${notification.kind}`} key={notification.id}>
          <div>
            <strong>{notification.title}</strong>
            {notification.message && <p>{notification.message}</p>}
          </div>
          <button
            type="button"
            className="icon-button"
            onClick={() => removeNotification(notification.id)}
            aria-label="Dismiss notification"
          >
            x
          </button>
        </article>
      ))}
    </aside>
  )
}
