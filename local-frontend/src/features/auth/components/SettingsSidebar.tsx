import { BackendModeBadge } from '../../../shared/config/BackendModeBadge'
import { displayRole } from '../../../shared/security/roles'
import { useAuthStore } from '../authStore'

export function SettingsSidebar() {
  const userId = useAuthStore((state) => state.userId)
  const tenantId = useAuthStore((state) => state.tenantId)
  const roles = useAuthStore((state) => state.roles)

  return (
    <section className="settings-sidebar" aria-label="설정">
      <div className="settings-nav-item is-active">
        <span>연결</span>
        <BackendModeBadge />
      </div>
      <dl className="session-summary">
        <div>
          <dt>User</dt>
          <dd>{userId ?? '-'}</dd>
        </div>
        <div>
          <dt>Tenant</dt>
          <dd>{tenantId ?? '-'}</dd>
        </div>
        <div>
          <dt>Role</dt>
          <dd>{displayRole(roles)}</dd>
        </div>
      </dl>
    </section>
  )
}
