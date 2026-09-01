import { BackendModeBadge } from '../../../shared/config/BackendModeBadge'
import { displayRole } from '../../../shared/security/roles'
import { useAuthStore } from '../authStore'

export function SettingsSidebar() {
  const userId = useAuthStore((state) => state.userId)
  const tenantId = useAuthStore((state) => state.tenantId)
  const roles = useAuthStore((state) => state.roles)

  return (
    <section className="grid content-start gap-[4px]" aria-label="설정">
      <div className="flex min-h-[46px] items-center justify-between gap-[10px] rounded-md border border-solid border-(--accent-line) bg-(--accent-weak) px-[12px] py-[10px] font-bold">
        <span>연결</span>
        <BackendModeBadge />
      </div>
      <dl className="m-0 mt-[20px] grid gap-[10px]">
        <div className="min-w-0 rounded-sm border border-solid border-border bg-bg-sub p-[11px]">
          <dt className="text-[11px] font-bold uppercase text-text-faint">User</dt>
          <dd className="m-0 mt-[3px] font-mono text-[12px] [overflow-wrap:anywhere]">{userId ?? '-'}</dd>
        </div>
        <div className="min-w-0 rounded-sm border border-solid border-border bg-bg-sub p-[11px]">
          <dt className="text-[11px] font-bold uppercase text-text-faint">Tenant</dt>
          <dd className="m-0 mt-[3px] font-mono text-[12px] [overflow-wrap:anywhere]">{tenantId ?? '-'}</dd>
        </div>
        <div className="min-w-0 rounded-sm border border-solid border-border bg-bg-sub p-[11px]">
          <dt className="text-[11px] font-bold uppercase text-text-faint">Role</dt>
          <dd className="m-0 mt-[3px] font-mono text-[12px] [overflow-wrap:anywhere]">
            {displayRole(roles)}
          </dd>
        </div>
      </dl>
    </section>
  )
}
