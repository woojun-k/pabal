import { useMemo, useState } from 'react'
import { DevAuthPanel } from './features/auth/DevAuthPanel'
import { useAuthStore } from './features/auth/authStore'
import { MessagePanel } from './features/messages/MessagePanel'
import { NotificationTray } from './features/notifications/NotificationTray'
import { useNotificationStore } from './features/notifications/notificationStore'
import { RealtimeBridge } from './features/realtime/RealtimeBridge'
import { RealtimeStatusPanel } from './features/realtime/RealtimeStatusPanel'
import { RoomSidebar } from './features/rooms/RoomSidebar'
import { useRoomStore } from './features/rooms/roomStore'
import { BackendModeBadge } from './shared/config/BackendModeBadge'
import { displayRole } from './shared/security/roles'
import type { UUID } from './shared/types/api'
import { formatDateTime } from './shared/utils/dateTime'
import './App.css'

type AppTab = 'contacts' | 'messages' | 'etc'

const appTabs: Array<{ id: AppTab; label: string; shortLabel: string }> = [
  { id: 'contacts', label: 'Contacts', shortLabel: 'CO' },
  { id: 'messages', label: 'Messages', shortLabel: 'MS' },
  { id: 'etc', label: 'Etc', shortLabel: 'ET' },
]

function App() {
  const [activeTab, setActiveTab] = useState<AppTab>('messages')
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false)
  const accessToken = useAuthStore((state) => state.accessToken)
  const userId = useAuthStore((state) => state.userId)
  const tenantId = useAuthStore((state) => state.tenantId)
  const roles = useAuthStore((state) => state.roles)
  const rooms = useRoomStore((state) => state.rooms)
  const activeRoomId = useRoomStore((state) => state.activeRoomId)
  const selectRoom = useRoomStore((state) => state.selectRoom)
  const clearNotificationsForRoom = useNotificationStore((state) => state.clearNotificationsForRoom)
  const directRooms = useMemo(() => rooms.filter((room) => room.type === 'DIRECT'), [rooms])
  const activeRoom = useMemo(
    () => rooms.find((room) => room.roomId === activeRoomId) ?? null,
    [activeRoomId, rooms],
  )
  const unreadTotal = rooms.reduce((total, room) => total + room.unreadCount, 0)

  const openRoom = (roomId: UUID) => {
    setActiveTab('messages')
    setIsSidebarCollapsed(false)
    clearNotificationsForRoom(roomId)
    void selectRoom(roomId)
  }

  const sidebarTitle = {
    contacts: 'Contacts',
    messages: 'Messages',
    etc: 'Etc',
  }[activeTab]

  const renderSidebarBody = () => {
    if (activeTab === 'messages') {
      if (!accessToken) {
        return (
          <section className="room-sidebar" aria-label="Chat rooms">
            <div className="sidebar-section-header">
              <span>Rooms</span>
            </div>
            <p className="empty-text">Issue a local token in Etc.</p>
          </section>
        )
      }

      return <RoomSidebar />
    }

    if (activeTab === 'contacts') {
      return (
        <section className="contact-sidebar" aria-label="Contacts">
          <div className="sidebar-section-header">
            <span>Direct contacts</span>
            <strong>{directRooms.length}</strong>
          </div>

          <div className="contact-list">
            {!accessToken && <p className="empty-text">Issue a local token in Etc.</p>}
            {accessToken && directRooms.length === 0 && (
              <p className="empty-text">No direct contacts yet.</p>
            )}
            {directRooms.map((room) => (
              <button
                type="button"
                className="contact-item"
                key={room.roomId}
                onClick={() => openRoom(room.roomId)}
              >
                <span className="avatar-token">{room.name?.slice(0, 2).toUpperCase() || 'DM'}</span>
                <span>
                  <strong>{room.name || room.roomId}</strong>
                  <small>{room.status}</small>
                </span>
              </button>
            ))}
          </div>
        </section>
      )
    }

    return (
      <section className="settings-sidebar" aria-label="Settings">
        <div className="settings-nav-item is-active">
          <span>Connection</span>
          <BackendModeBadge />
        </div>
        <dl className="session-summary">
          <div>
            <dt>User</dt>
            <dd>{userId ?? '-'}</dd>
          </div>
          <div>
            <dt>Tenant</dt>
            <dd>{tenantId ?? '-'}</dd>
          </div>
          <div>
            <dt>Role</dt>
            <dd>{displayRole(roles)}</dd>
          </div>
        </dl>
      </section>
    )
  }

  const renderMain = () => {
    if (activeTab === 'messages') {
      if (!accessToken) {
        return (
          <section className="content-panel locked-panel">
            <p className="eyebrow">Messages</p>
            <h2>Issue a local token first</h2>
          </section>
        )
      }

      return (
        <MessagePanel
          activeRoomId={activeRoomId}
          currentUserId={userId}
          key={activeRoomId ?? 'empty-room'}
        />
      )
    }

    if (activeTab === 'contacts') {
      return (
        <section className="content-panel contacts-main">
          <div className="content-header">
            <div>
              <p className="eyebrow">Contacts</p>
              <h2>Direct contacts</h2>
            </div>
            <span className="status-pill">{directRooms.length} contacts</span>
          </div>

          <div className="contact-grid">
            {!accessToken && (
              <article className="empty-state">
                <h3>Local session required</h3>
                <p>Open Etc and issue a local token.</p>
              </article>
            )}
            {accessToken && directRooms.length === 0 && (
              <article className="empty-state">
                <h3>No contacts yet</h3>
                <p>Create a direct room from Messages.</p>
              </article>
            )}
            {directRooms.map((room) => (
              <article className="contact-card" key={room.roomId}>
                <span className="avatar-token large">
                  {room.name?.slice(0, 2).toUpperCase() || 'DM'}
                </span>
                <div>
                  <h3>{room.name || room.roomId}</h3>
                  <p>{room.unreadCount > 0 ? `${room.unreadCount} unread` : room.status}</p>
                  <small>{formatDateTime(room.lastMessageAt ?? room.joinedAt)}</small>
                </div>
                <button type="button" className="secondary compact" onClick={() => openRoom(room.roomId)}>
                  Message
                </button>
              </article>
            ))}
          </div>
        </section>
      )
    }

    return (
      <section className="settings-main">
        <DevAuthPanel />
        <RealtimeStatusPanel />
      </section>
    )
  }

  return (
    <main className={`app-shell ${isSidebarCollapsed ? 'is-sidebar-collapsed' : ''}`}>
      <RealtimeBridge />
      <NotificationTray />

      <aside className="app-nav" aria-label="Workspace navigation">
        <nav className="app-tabs">
          {appTabs.map((tab) => (
            <button
              type="button"
              className={activeTab === tab.id ? 'app-tab is-active' : 'app-tab'}
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              aria-pressed={activeTab === tab.id}
            >
              <span>{tab.shortLabel}</span>
              <small>{tab.label}</small>
              {tab.id === 'messages' && unreadTotal > 0 && <em>{unreadTotal}</em>}
            </button>
          ))}
        </nav>
        <div className={accessToken ? 'nav-session is-ready' : 'nav-session'}>
          {accessToken ? 'on' : 'off'}
        </div>
      </aside>

      <aside className="workspace-sidebar" aria-label={`${sidebarTitle} sidebar`}>
        <div className="sidebar-chrome">
          <div>
            <p className="eyebrow">{sidebarTitle}</p>
            <h1>{activeTab === 'messages' ? activeRoom?.name || 'Rooms' : sidebarTitle}</h1>
          </div>
          <button
            type="button"
            className="icon-button"
            onClick={() => setIsSidebarCollapsed((value) => !value)}
            aria-label={isSidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          >
            {isSidebarCollapsed ? '>' : '<'}
          </button>
        </div>
        <div className="sidebar-body">{renderSidebarBody()}</div>
      </aside>

      <section className="workspace-main" aria-label={`${sidebarTitle} main content`}>
        {renderMain()}
      </section>
    </main>
  )
}

export default App
