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
import './App.css'

type AppTab = 'messages' | 'contacts' | 'etc'

const railTabs: Array<{ id: AppTab; label: string; icon: string }> = [
  { id: 'messages', label: '메시지', icon: '#' },
  { id: 'contacts', label: '연락처', icon: '✉' },
  { id: 'etc', label: '설정', icon: '⚙' },
]

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

      <aside className="rail" aria-label="워크스페이스 내비게이션">
        <button type="button" className="rail-ws active" title="아이누리">
          아
        </button>
        <button type="button" className="rail-ws" title="사이드 프로젝트">
          SP
        </button>
        <button type="button" className="rail-ws" title="스터디">
          스
        </button>
        <div className="rail-nav" title="워크스페이스 추가">＋</div>
        <div className="rail-div" />
        {railTabs.map((tab) => (
          <button
            type="button"
            className={activeTab === tab.id ? 'rail-nav is-active' : 'rail-nav'}
            key={tab.id}
            title={tab.label}
            onClick={() => setActiveTab(tab.id)}
            aria-pressed={activeTab === tab.id}
          >
            {tab.icon}
            {tab.id === 'messages' && unreadTotal > 0 && <span className="rail-pip">{unreadTotal}</span>}
          </button>
        ))}
        <span className={accessToken ? 'nav-session is-ready' : 'nav-session'}>
          {accessToken ? 'on' : 'off'}
        </span>
      </aside>

      <aside className="sidebar" aria-label="워크스페이스 사이드바">
        <div className="ws-head">
          <span className="av sm">우</span>
          <span className="nm display">아이누리</span>
          <button type="button" className="icobtn" title="새 메시지">✎</button>
          <span className="chev">▾</span>
        </div>
        <div className="sb-search">
          <span>🔍</span>
          <span>검색 또는 점프</span>
          <kbd>⌘K</kbd>
        </div>
        <div className="sb-scroll">{renderSidebar()}</div>
        <div className="me-bar">
          <div className="av-pos">
            <span className="av sm">우</span>
            <span className="presence p-online" />
          </div>
          <div className="me-text">
            <div className="nm">정우</div>
            <div className="st">{accessToken ? '집중 모드' : '오프라인'}</div>
          </div>
          <button type="button" className="icobtn" onClick={() => setActiveTab('etc')} title="설정">
            ⚙
          </button>
        </div>
      </aside>

      {renderMain()}
    </main>
  )
}

export default App
