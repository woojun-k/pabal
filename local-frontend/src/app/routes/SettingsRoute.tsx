import { DevAuthPanel } from '../../features/auth/DevAuthPanel'
import { RealtimeStatusPanel } from '../../features/realtime/RealtimeStatusPanel'

export function SettingsRoute() {
  return (
    <section className="main settings-main">
      <DevAuthPanel />
      <RealtimeStatusPanel />
    </section>
  )
}
