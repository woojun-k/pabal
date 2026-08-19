import { useAuthStore } from '../../features/auth/authStore'
import { MessagePanel } from '../../features/messages/MessagePanel'
import { useRoomStore } from '../../features/rooms/roomStore'
import { Button } from '../../shared/ui/Button'
import { useRoomRouteSync } from './useRoomRouteSync'

export function MessagesRoute() {
  useRoomRouteSync()
  const userId = useAuthStore((state) => state.userId)
  const activeRoomId = useRoomStore((state) => state.activeRoomId)
  const loadStatus = useRoomStore((state) => state.loadStatus)
  const loadRooms = useRoomStore((state) => state.loadRooms)

  if (!activeRoomId && (loadStatus === 'idle' || loadStatus === 'loading')) {
    return (
      <section className="main empty-main">
        <div className="empty-chat">
          <div className="av lg">#</div>
          <h2>방 목록을 불러오는 중...</h2>
          <p>잠시만 기다려주세요.</p>
        </div>
      </section>
    )
  }

  if (!activeRoomId && loadStatus === 'error') {
    return (
      <section className="main empty-main">
        <div className="empty-chat">
          <div className="av lg">!</div>
          <h2>방 목록을 불러오지 못했습니다</h2>
          <p>네트워크 상태를 확인한 뒤 다시 시도하세요.</p>
          <Button variant="ghost" size="compact" onClick={() => void loadRooms()}>
            다시 시도
          </Button>
        </div>
      </section>
    )
  }

  return <MessagePanel activeRoomId={activeRoomId} currentUserId={userId} />
}
