import type { ReactNode } from 'react'

interface SidebarFrameProps {
  sessionReady: boolean
  onOpenSettings: () => void
  children: ReactNode
}

export function SidebarFrame({ sessionReady, onOpenSettings, children }: SidebarFrameProps) {
  return (
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
      <div className="sb-scroll">{children}</div>
      <div className="me-bar">
        <div className="av-pos">
          <span className="av sm">우</span>
          <span className="presence p-online" />
        </div>
        <div className="me-text">
          <div className="nm">정우</div>
          <div className="st">{sessionReady ? '집중 모드' : '오프라인'}</div>
        </div>
        <button type="button" className="icobtn" onClick={onOpenSettings} title="설정">
          ⚙
        </button>
      </div>
    </aside>
  )
}
