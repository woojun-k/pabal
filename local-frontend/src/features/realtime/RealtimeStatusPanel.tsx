import { BackendModeBadge } from '../../shared/config/BackendModeBadge'
import { Button } from '../../shared/ui/Button'
import { useRealtimeStore } from './realtimeStore'

export function RealtimeStatusPanel() {
  const { status, error, subscriptionIds, connect, disconnect } = useRealtimeStore()

  return (
    <section className="panel realtime-panel">
      <div className="section-header">
        <div>
          <p className="eyebrow">
            Realtime
            <BackendModeBadge />
          </p>
          <h2>{status}</h2>
        </div>
        <div className="button-row compact-row">
          <Button type="button" variant="ghost" size="compact" onClick={connect}>
            Connect
          </Button>
          <Button
            type="button"
            variant="ghost"
            size="compact"
            onClick={() => void disconnect()}
          >
            Disconnect
          </Button>
        </div>
      </div>
      <p className="muted-text">{subscriptionIds.length} subscriptions</p>
      {error && <p className="error-text">{error.message}</p>}
    </section>
  )
}
