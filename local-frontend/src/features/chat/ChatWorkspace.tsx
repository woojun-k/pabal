import { useAuthStore } from '../auth/authStore'
import { MessagePanel } from '../messages/MessagePanel'
import { RoomSidebar } from '../rooms/RoomSidebar'
import { useRoomStore } from '../rooms/roomStore'

export function ChatWorkspace() {
  const accessToken = useAuthStore((state) => state.accessToken)
  const userId = useAuthStore((state) => state.userId)
  const activeRoomId = useRoomStore((state) => state.activeRoomId)

  if (!accessToken) {
    return (
      <section className="panel locked-panel">
        <p className="eyebrow">Chat</p>
        <h2>Issue a local token first</h2>
      </section>
    )
  }

  return (
    <section className="chat-workspace">
      <RoomSidebar />
      <MessagePanel
        activeRoomId={activeRoomId}
        currentUserId={userId}
        key={activeRoomId ?? 'empty-room'}
      />
    </section>
  )
}
