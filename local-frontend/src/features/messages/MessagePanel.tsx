import { useEffect, useMemo, useRef, useState } from 'react'
import type { CSSProperties, FormEvent } from 'react'
import { apiContract } from '../../shared/constants/apiContract'
import type { UUID } from '../../shared/types/api'
import { useNotificationStore } from '../notifications/notificationStore'
import { useRoomStore } from '../rooms/roomStore'
import { useMessageStore, type MessageView } from './messageStore'

type MessagePanelProps = {
  activeRoomId: UUID | null
  currentUserId: UUID | null
}

const avatarColors = ['#6b8e9e', '#a9789f', '#c08552', '#5b9e78', '#8a86c4', '#7d8aa3']

const getRoomName = (roomId: UUID, roomName?: string) => roomName || roomId

const formatMessageTime = (value: string) =>
  new Intl.DateTimeFormat('ko-KR', {
    hour: 'numeric',
    minute: '2-digit',
  }).format(new Date(value))

const getSenderLabel = (senderId: UUID, currentUserId: UUID | null) => {
  if (senderId === currentUserId) {
    return '나'
  }

  return `사용자 ${senderId.slice(-4)}`
}

const getAvatarText = (senderId: UUID, currentUserId: UUID | null) => {
  if (senderId === currentUserId) {
    return '나'
  }

  return senderId.replaceAll('-', '').slice(-2).toUpperCase()
}

const getAvatarStyle = (senderId: UUID): CSSProperties => {
  const value = [...senderId].reduce((total, char) => total + char.charCodeAt(0), 0)
  return { '--av': avatarColors[value % avatarColors.length] } as CSSProperties
}

export function MessagePanel({ activeRoomId, currentUserId }: MessagePanelProps) {
  const activeRoom = useRoomStore((state) =>
    activeRoomId ? state.rooms.find((room) => room.roomId === activeRoomId) ?? null : null,
  )
  const {
    rooms,
    isLoading,
    isSending,
    error,
    loadMessages,
    sendTextMessage,
    editTextMessage,
    deleteTextMessage,
    markRoomRead,
  } = useMessageStore()
  const addNotification = useNotificationStore((state) => state.addNotification)
  const [content, setContent] = useState('')
  const [editingMessage, setEditingMessage] = useState<MessageView | null>(null)
  const composerRef = useRef<HTMLTextAreaElement | null>(null)
  const roomMessages = useMemo(
    () => (activeRoomId ? rooms[activeRoomId]?.messages ?? [] : []),
    [activeRoomId, rooms],
  )

  useEffect(() => {
    if (!activeRoomId) {
      return
    }

    void loadMessages(activeRoomId, true)
  }, [activeRoomId, loadMessages])

  useEffect(() => {
    const textarea = composerRef.current

    if (!textarea) {
      return
    }

    textarea.style.height = 'auto'
    textarea.style.height = `${Math.min(textarea.scrollHeight, 140)}px`
  }, [content])

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (!activeRoomId || !currentUserId || !content.trim()) {
      return
    }

    await sendTextMessage(activeRoomId, currentUserId, content.trim())
    setContent('')
  }

  const handleEditSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (!activeRoomId || !editingMessage || !content.trim()) {
      return
    }

    await editTextMessage(activeRoomId, editingMessage.messageId, content.trim())
    setEditingMessage(null)
    setContent('')
  }

  const startEdit = (message: MessageView) => {
    setEditingMessage(message)
    setContent(message.content)
  }

  const handleDelete = async (message: MessageView) => {
    if (!activeRoomId) {
      return
    }

    await deleteTextMessage(activeRoomId, message.messageId)
  }

  const handleMarkRead = async () => {
    if (!activeRoomId) {
      return
    }

    await markRoomRead(activeRoomId)
    addNotification({
      kind: 'success',
      title: '읽음 처리했습니다',
      roomId: activeRoomId,
    })
  }

  if (!activeRoomId) {
    return (
      <section className="main empty-main">
        <div className="empty-chat">
          <div className="av lg">#</div>
          <h2>채팅방을 선택하세요</h2>
          <p>왼쪽 사이드바에서 채널 또는 다이렉트 메시지를 선택하면 대화가 표시됩니다.</p>
        </div>
      </section>
    )
  }

  const title = getRoomName(activeRoomId, activeRoom?.name)
  const isChannel = activeRoom?.type === 'CHANNEL'

  return (
    <section className="main chat-main">
      <header className="chead">
        <div className="ttl">
          <span className="glyph">{isChannel ? '#' : '●'}</span>
          <h1>{title}</h1>
        </div>
        <div className="topic">
          {isChannel ? '스프린트 24 · 실시간 채팅 백엔드' : '다이렉트 메시지'}
        </div>
        <div className="tools">
          <div className="head-search">
            <span className="ico">🔍</span>
            <input placeholder={`${isChannel ? '#' : ''}${title} 검색`} />
            <kbd>⌘F</kbd>
          </div>
          <button type="button" className="icobtn" title="읽음 처리" onClick={handleMarkRead}>
            ✓
          </button>
          <button type="button" className="icobtn" title="알림">
            🔔
          </button>
          <button type="button" className="icobtn wide" title="멤버">
            👥 <span>{isChannel ? '18' : '2'}</span>
          </button>
          <button type="button" className="icobtn" title="더보기">
            ⋯
          </button>
        </div>
      </header>

      {error && <p className="error-text chat-error">{error.message}</p>}

      <div className="stream" aria-busy={isLoading}>
        <div className="stream-inner">
          <div className="channel-intro">
            <div className="av lg channel-mark">{isChannel ? '#' : '●'}</div>
            <h2>{isChannel ? `#${title}` : title}</h2>
            <p>{isChannel ? `#${title} 채널의 시작입니다.` : `${title} 대화의 시작입니다.`}</p>
          </div>

          <div className="day">
            <span>오늘</span>
          </div>

          {roomMessages.length === 0 && (
            <p className="empty-text chat-empty">{isLoading ? '메시지를 불러오는 중...' : '아직 메시지가 없습니다.'}</p>
          )}
          {roomMessages.map((message, index) => {
            const previous = roomMessages[index - 1]
            const grouped = previous?.senderId === message.senderId
            const mine = message.senderId === currentUserId

            return (
              <article
                className={['bwrap', mine ? 'mine' : '', grouped ? 'grouped' : ''].filter(Boolean).join(' ')}
                key={`${message.messageId}:${message.clientMessageId}`}
              >
                <div className="av-slot">
                  {!grouped && (
                    <span className="av" style={getAvatarStyle(message.senderId)}>
                      {getAvatarText(message.senderId, currentUserId)}
                    </span>
                  )}
                </div>
                <div className="bcol">
                  <div className="hov">
                    <button type="button" title="반응">😀</button>
                    <button type="button" title="답글">↩</button>
                    {message.status !== 'DELETED' && mine && (
                      <button type="button" title="수정" onClick={() => startEdit(message)}>
                        ✎
                      </button>
                    )}
                    {message.status !== 'DELETED' && mine && (
                      <button type="button" title="삭제" onClick={() => void handleDelete(message)}>
                        ⋯
                      </button>
                    )}
                  </div>
                  {!grouped && (
                    <div className="bmeta">
                      <span className="au">{getSenderLabel(message.senderId, currentUserId)}</span>
                      <span className="role">{mine ? '나' : '멤버'}</span>
                      <time className="ts">{formatMessageTime(message.createdAt)}</time>
                    </div>
                  )}
                  <div className={message.status === 'DELETED' ? 'bubble del' : 'bubble'}>
                    {message.status === 'DELETED' ? '삭제된 메시지입니다' : message.content}
                    {message.status === 'EDITED' && <span className="edited">(수정됨)</span>}
                  </div>
                  {message.deliveryStatus && message.deliveryStatus !== 'sent' && (
                    <div className="line-ts">{message.deliveryStatus}</div>
                  )}
                </div>
              </article>
            )
          })}
        </div>
      </div>

      <form className="composer" onSubmit={editingMessage ? handleEditSubmit : handleSubmit}>
        <div className="composer-stack">
          <div className="composer-inner">
            <textarea
              ref={composerRef}
              rows={1}
              value={content}
              onChange={(event) => setContent(event.target.value)}
              placeholder={editingMessage ? '메시지 수정' : `${isChannel ? `#${title}` : title} 에 메시지 보내기`}
              maxLength={apiContract.message.contentMaxLength}
            />
            <div className="ctrls">
              <button type="button" className="icobtn" title="첨부">＋</button>
              <button type="button" className="icobtn" title="이모지">😀</button>
              <button type="button" className="icobtn" title="멘션">@</button>
              <span className="spacer" />
              {editingMessage && (
                <button
                  type="button"
                  className="btn-ghost compact"
                  onClick={() => {
                    setEditingMessage(null)
                    setContent('')
                  }}
                >
                  취소
                </button>
              )}
              <span className="hint">⏎ 전송 · ⇧⏎ 줄바꿈</span>
              <button type="submit" className="send" disabled={isSending || !content.trim()}>
                {editingMessage ? '저장' : '보내기'}
              </button>
            </div>
          </div>
        </div>
      </form>
    </section>
  )
}
