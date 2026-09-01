import type { ReactNode } from 'react'

interface SidebarFrameProps {
  hasSession: boolean
  onOpenSettings: () => void
  children: ReactNode
}

export function SidebarFrame({ hasSession, onOpenSettings, children }: SidebarFrameProps) {
  return (
    <aside
      className="flex min-h-0 w-[252px] flex-[0_0_252px] flex-col border-0 border-r border-solid border-border bg-sidebar [@media(max-width:980px)]:w-auto [@media(max-width:980px)]:border-r-0 [@media(max-width:980px)]:border-b"
      aria-label="워크스페이스 사이드바"
    >
      <div className="flex min-h-[60px] items-center gap-[9px] border-0 border-b border-solid border-border px-[16px] py-[13px]">
        <span className="av sm">우</span>
        <span className="min-w-0 truncate font-display text-[18px] font-bold">아이누리</span>
        <button type="button" className="icobtn" title="새 메시지">✎</button>
        <span className="ml-auto text-[12px] text-text-faint">▾</span>
      </div>
      <div className="mx-[12px] mb-[6px] mt-[12px] flex min-h-[38px] items-center gap-[8px] rounded-md border border-solid border-border-strong bg-bg px-[11px] py-[8px] text-[13.5px] text-text-faint">
        <span>🔍</span>
        <span className="flex-1 truncate">검색 또는 점프</span>
        <kbd className="rounded-[5px] border border-solid border-border-strong px-[5px] py-[1px] font-mono text-[11px] text-text-faint">⌘K</kbd>
      </div>
      <div className="min-h-0 flex-1 overflow-x-hidden overflow-y-auto px-[8px] pb-[12px] pt-[6px] [&::-webkit-scrollbar-thumb]:rounded-[6px] [&::-webkit-scrollbar-thumb]:border-[3px] [&::-webkit-scrollbar-thumb]:border-solid [&::-webkit-scrollbar-thumb]:border-sidebar [&::-webkit-scrollbar-thumb]:bg-border-strong [&::-webkit-scrollbar]:w-[10px]">
        {children}
      </div>
      <div className="flex items-center gap-[10px] border-0 border-t border-solid border-border bg-sidebar px-[12px] py-[10px]">
        <div className="relative">
          <span className="av sm">우</span>
          <span className="presence p-online absolute right-[-2px] bottom-[-2px]" />
        </div>
        <div className="min-w-0 flex-1">
          <div className="truncate text-[14px] font-semibold">정우</div>
          <div className="text-[12px] text-text-muted">{hasSession ? '집중 모드' : '오프라인'}</div>
        </div>
        <button type="button" className="icobtn" onClick={onOpenSettings} title="설정">
          ⚙
        </button>
      </div>
    </aside>
  )
}
