import { useEffect, useMemo } from 'react'
import { Outlet, useLocation } from 'react-router'
import { useAuthStore } from '../../features/auth/authStore'
import { SettingsSidebar } from '../../features/auth/components/SettingsSidebar'
import { NotificationTray } from '../../features/notifications/NotificationTray'
import { RealtimeBridge } from '../../features/realtime/RealtimeBridge'
import { ContactsSidebar } from '../../features/rooms/components/ContactsSidebar'
import { RoomSidebar } from '../../features/rooms/components/RoomSidebar'
import { useRoomStore } from '../../features/rooms/roomStore'
import { useAppNavigation } from '../navigation'
import { deriveTab } from '../paths'
import { SidebarFrame } from './SidebarFrame'
import { WorkspaceRail } from './WorkspaceRail'

export function AppLayout() {
  const location = useLocation()
  const activeTab = deriveTab(location.pathname)
  const accessToken = useAuthStore((state) => state.accessToken)
  const rooms = useRoomStore((state) => state.rooms)
  const resetRooms = useRoomStore((state) => state.resetRooms)
  const { goToRoom, goToTab } = useAppNavigation()
  const directRooms = useMemo(
    () => rooms.filter((room) => room.type === 'DIRECT' || room.type === 'GROUP'),
    [rooms],
  )
  const unreadTotal = rooms.reduce((total, room) => total + room.unreadCount, 0)

  useEffect(() => {
    resetRooms()
  }, [accessToken, resetRooms])

  const renderSidebar = () => {
    if (activeTab === 'messages') {
      if (!accessToken) {
        return <p className="empty-text sb-empty">설정에서 로컬 토큰을 발급하세요.</p>
      }

      return <RoomSidebar onSelectRoom={goToRoom} />
    }

    if (activeTab === 'contacts') {
      return (
        <ContactsSidebar
          directRooms={directRooms}
          hasSession={Boolean(accessToken)}
          onOpenRoom={goToRoom}
        />
      )
    }

    return <SettingsSidebar />
  }

  return (
    <main className="app-shell" data-theme="light">
      <RealtimeBridge />
      <NotificationTray />
      <WorkspaceRail
        activeTab={activeTab}
        unreadTotal={unreadTotal}
        hasSession={Boolean(accessToken)}
        onSelectTab={goToTab}
      />
      <SidebarFrame hasSession={Boolean(accessToken)} onOpenSettings={() => goToTab('etc')}>
        {renderSidebar()}
      </SidebarFrame>
      <Outlet />
    </main>
  )
}
