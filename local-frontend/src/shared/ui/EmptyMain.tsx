import type { ReactNode } from 'react'

type EmptyMainProps = {
  glyph: string
  title: string
  children?: ReactNode
}

export function EmptyMain({ glyph, title, children }: EmptyMainProps) {
  return (
    <section className="main empty-main">
      <div className="empty-chat">
        <div className="av lg">{glyph}</div>
        <h2>{title}</h2>
        {children}
      </div>
    </section>
  )
}
