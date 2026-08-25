import { useAuthStore } from '../../features/auth/authStore'
import { MessagePanel } from '../../features/messages/MessagePanel'
import { useRoomStore } from '../../features/rooms/roomStore'
import { Button } from '../../shared/ui/Button'
import { EmptyMain } from '../../shared/ui/EmptyMain'
import { useRoomRouteSync } from './useRoomRouteSync'

export function MessagesRoute() {
  useRoomRouteSync()
  const userId = useAuthStore((state) => state.userId)
  const activeRoomId = useRoomStore((state) => state.activeRoomId)
  const loadStatus = useRoomStore((state) => state.loadStatus)
  const loadRooms = useRoomStore((state) => state.loadRooms)
  const error = useRoomStore((state) => state.error)

  if (!activeRoomId && loadStatus !== 'ready') {
    if (loadStatus === 'error') {
      return (
        <EmptyMain glyph="!" title="방 목록을 불러오지 못했습니다">
          <p>{error?.message ?? '네트워크 상태를 확인한 뒤 다시 시도하세요.'}</p>
          <Button type="button" variant="ghost" size="compact" onClick={() => void loadRooms()}>
            다시 시도
          </Button>
        </EmptyMain>
      )
    }

    return (
      <EmptyMain glyph="#" title="방 목록을 불러오는 중...">
        <p>잠시만 기다려주세요.</p>
      </EmptyMain>
    )
  }

  return <MessagePanel activeRoomId={activeRoomId} currentUserId={userId} />
}
