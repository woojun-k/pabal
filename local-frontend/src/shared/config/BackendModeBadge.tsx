import { env } from './env'

export function BackendModeBadge() {
  return (
    <span className={`mode-badge is-${env.backendMode}`}>
      {env.backendMode}
    </span>
  )
}
