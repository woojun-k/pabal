import { BackendModeBadge } from '../../shared/config/BackendModeBadge'
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
          <button type="button" className="secondary compact" onClick={connect}>
            Connect
          </button>
          <button type="button" className="secondary compact" onClick={() => void disconnect()}>
            Disconnect
          </button>
        </div>
      </div>
      <p className="muted-text">{subscriptionIds.length} subscriptions</p>
      {error && <p className="error-text">{error.message}</p>}
    </section>
  )
}
