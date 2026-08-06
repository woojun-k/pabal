import { useMemo, useState } from 'react'
import { DevAuthPanel } from '../features/auth/DevAuthPanel'
import { useAuthStore } from '../features/auth/authStore'
import { SettingsSidebar } from '../features/auth/components/SettingsSidebar'
import { MessagePanel } from '../features/messages/MessagePanel'
import { NotificationTray } from '../features/notifications/NotificationTray'
import { useNotificationStore } from '../features/notifications/notificationStore'
import { RealtimeBridge } from '../features/realtime/RealtimeBridge'
import { RealtimeStatusPanel } from '../features/realtime/RealtimeStatusPanel'
import { RoomSidebar } from '../features/rooms/RoomSidebar'
import { ContactsMain } from '../features/rooms/components/ContactsMain'
import { ContactsSidebar } from '../features/rooms/components/ContactsSidebar'
import { useRoomStore } from '../features/rooms/roomStore'
import type { UUID } from '../shared/types/api'
import { SidebarFrame } from './layout/SidebarFrame'
import { WorkspaceRail, type AppTab } from './layout/WorkspaceRail'

function App() {
  const [activeTab, setActiveTab] = useState<AppTab>('messages')
  const accessToken = useAuthStore((state) => state.accessToken)
  const userId = useAuthStore((state) => state.userId)
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
        <ContactsSidebar
          directRooms={directRooms}
          hasSession={Boolean(accessToken)}
          onOpenRoom={openRoom}
        />
      )
    }

    return <SettingsSidebar />
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
      return <ContactsMain directRooms={directRooms} onOpenRoom={openRoom} />
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
