import { useEffect, useMemo, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { apiContract } from '../../shared/constants/apiContract'
import type { UUID } from '../../shared/types/api'
import { formatDateTime } from '../../shared/utils/dateTime'
import { useNotificationStore } from '../notifications/notificationStore'
import { useMessageStore, type MessageView } from './messageStore'

type MessagePanelProps = {
  activeRoomId: UUID | null
  currentUserId: UUID | null
}

export function MessagePanel({ activeRoomId, currentUserId }: MessagePanelProps) {
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
    textarea.style.height = `${textarea.scrollHeight}px`
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
      title: 'Marked as read',
    })
  }

  if (!activeRoomId) {
    return (
      <section className="message-panel empty-panel">
        <p className="eyebrow">Messages</p>
        <h2>Select or create a room</h2>
      </section>
    )
  }

  return (
    <section className="message-panel">
      <div className="section-header">
        <div>
          <p className="eyebrow">Messages</p>
          <h2>{activeRoomId}</h2>
        </div>
        <button type="button" className="secondary compact" onClick={handleMarkRead}>
          Read
        </button>
      </div>

      {error && <p className="error-text">{error.message}</p>}

      <div className="message-list" aria-busy={isLoading}>
        {roomMessages.length === 0 && (
          <p className="empty-text">{isLoading ? 'Loading messages...' : 'No messages yet'}</p>
        )}
        {roomMessages.map((message) => (
          <article
            className={message.senderId === currentUserId ? 'message-bubble mine' : 'message-bubble'}
            key={`${message.messageId}:${message.clientMessageId}`}
          >
            <div className="message-meta">
              <span>{message.senderId === currentUserId ? 'me' : message.senderId}</span>
              <time>{formatDateTime(message.createdAt)}</time>
              {message.deliveryStatus && <em>{message.deliveryStatus}</em>}
            </div>
            <p>{message.status === 'DELETED' ? 'Deleted message' : message.content}</p>
            {message.status !== 'DELETED' && message.senderId === currentUserId && (
              <div className="message-actions">
                <button type="button" className="ghost-button" onClick={() => startEdit(message)}>
                  Edit
                </button>
                <button type="button" className="ghost-button danger" onClick={() => void handleDelete(message)}>
                  Delete
                </button>
              </div>
            )}
          </article>
        ))}
      </div>

      <form className="composer" onSubmit={editingMessage ? handleEditSubmit : handleSubmit}>
        <textarea
          ref={composerRef}
          rows={1}
          value={content}
          onChange={(event) => setContent(event.target.value)}
          placeholder={editingMessage ? 'Edit message' : 'Type a message'}
          maxLength={apiContract.message.contentMaxLength}
        />
        {editingMessage && (
          <button
            type="button"
            className="secondary compact"
            onClick={() => {
              setEditingMessage(null)
              setContent('')
            }}
          >
            Cancel
          </button>
        )}
        <button type="submit" disabled={isSending || !content.trim()}>
          {editingMessage ? 'Save' : 'Send'}
        </button>
      </form>
    </section>
  )
}
