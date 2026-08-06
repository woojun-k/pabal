import { useMemo, useState } from 'react'
import { DevAuthPanel } from '../features/auth/DevAuthPanel'
import { useAuthStore } from '../features/auth/authStore'
import { MessagePanel } from '../features/messages/MessagePanel'
import { NotificationTray } from '../features/notifications/NotificationTray'
import { useNotificationStore } from '../features/notifications/notificationStore'
import { RealtimeBridge } from '../features/realtime/RealtimeBridge'
import { RealtimeStatusPanel } from '../features/realtime/RealtimeStatusPanel'
import { RoomSidebar } from '../features/rooms/RoomSidebar'
import { useRoomStore } from '../features/rooms/roomStore'
import { BackendModeBadge } from '../shared/config/BackendModeBadge'
import { displayRole } from '../shared/security/roles'
import type { UUID } from '../shared/types/api'
import { SidebarFrame } from './layout/SidebarFrame'
import { WorkspaceRail, type AppTab } from './layout/WorkspaceRail'

const roomTitle = (roomName: string, fallback: string) => roomName || fallback

function App() {
  const [activeTab, setActiveTab] = useState<AppTab>('messages')
  const accessToken = useAuthStore((state) => state.accessToken)
  const userId = useAuthStore((state) => state.userId)
  const tenantId = useAuthStore((state) => state.tenantId)
  const roles = useAuthStore((state) => state.roles)
  const rooms = useRoomStore((state) => state.rooms)
  const activeRoomId = useRoomStore((state) => state.activeRoomId)
  const selectRoom = useRoomStore((state) => state.selectRoom)
  const clearNotificationsForRoom = useNotificationStore((state) => state.clearNotificationsForRoom)
  const directRooms = useMemo(
    () => rooms.filter((room) => room.type === 'DIRECT' || room.type === 'GROUP'),
    [rooms],
  )
  const unreadTotal = rooms.reduce((total, room) => total + room.unreadCount, 0)

  const openRoom = (roomId: UUID) => {
    setActiveTab('messages')
    clearNotificationsForRoom(roomId)
    void selectRoom(roomId)
  }

  const renderSidebar = () => {
    if (activeTab === 'messages') {
      if (!accessToken) {
        return <p className="empty-text sb-empty">설정에서 로컬 토큰을 발급하세요.</p>
      }

      return <RoomSidebar />
    }

    if (activeTab === 'contacts') {
      return (
        <section className="contact-sidebar" aria-label="연락처">
          <div className="grp">
            <span className="tri">▾</span>
            다이렉트 메시지
            <span className="pip muted">{directRooms.length}</span>
          </div>
          {directRooms.length === 0 && (
            <p className="empty-text sb-empty">
              {accessToken ? '아직 연락처가 없습니다.' : '설정에서 로컬 토큰을 발급하세요.'}
            </p>
          )}
          {directRooms.map((room) => (
            <button
              type="button"
              className="nav-item"
              key={room.roomId}
              onClick={() => openRoom(room.roomId)}
            >
              <span className={room.type === 'GROUP' ? 'presence p-group' : 'presence p-online'} />
              <span className="nm">{roomTitle(room.name, room.type === 'GROUP' ? '그룹 메시지' : '다이렉트 메시지')}</span>
              {room.unreadCount > 0 && <span className="pip">{room.unreadCount}</span>}
            </button>
          ))}
        </section>
      )
    }

    return (
      <section className="settings-sidebar" aria-label="설정">
        <div className="settings-nav-item is-active">
          <span>연결</span>
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
          <section className="main empty-main">
            <div className="empty-chat">
              <div className="av lg">#</div>
              <h2>메시지를 시작하려면 토큰이 필요합니다</h2>
              <p>왼쪽 레일의 설정에서 로컬 토큰을 발급하면 채팅방 목록이 표시됩니다.</p>
            </div>
          </section>
        )
      }

      return <MessagePanel activeRoomId={activeRoomId} currentUserId={userId} />
    }

    if (activeTab === 'contacts') {
      return (
        <section className="main contacts-main">
          <header className="chead">
            <div className="ttl">
              <span className="glyph">✉</span>
              <h1>연락처</h1>
            </div>
            <div className="topic">다이렉트 메시지와 그룹 대화를 빠르게 엽니다</div>
          </header>
          <div className="contact-grid">
            {directRooms.length === 0 && (
              <article className="empty-card">
                <div className="av lg">✉</div>
                <h2>연락처가 없습니다</h2>
                <p>메시지 탭에서 다이렉트 메시지를 만들면 여기에 표시됩니다.</p>
              </article>
            )}
            {directRooms.map((room) => (
              <article className="contact-card" key={room.roomId}>
                <span className="av">{room.type === 'GROUP' ? '그' : 'DM'}</span>
                <div>
                  <h3>{roomTitle(room.name, room.type === 'GROUP' ? '그룹 메시지' : '다이렉트 메시지')}</h3>
                  <p>{room.unreadCount > 0 ? `${room.unreadCount}개 안 읽음` : room.status}</p>
                </div>
                <button type="button" className="btn-ghost" onClick={() => openRoom(room.roomId)}>
                  메시지
                </button>
              </article>
            ))}
          </div>
        </section>
      )
    }

    return (
      <section className="main settings-main">
        <DevAuthPanel />
        <RealtimeStatusPanel />
      </section>
    )
  }

  return (
    <main className="app-shell" data-theme="light">
      <RealtimeBridge />
      <NotificationTray />
      <WorkspaceRail
        activeTab={activeTab}
        unreadTotal={unreadTotal}
        sessionReady={Boolean(accessToken)}
        onSelectTab={setActiveTab}
      />
      <SidebarFrame sessionReady={Boolean(accessToken)} onOpenSettings={() => setActiveTab('etc')}>
        {renderSidebar()}
      </SidebarFrame>
      {renderMain()}
    </main>
  )
}

export default App
