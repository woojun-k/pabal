import { Navigate, Outlet, useParams } from 'react-router'
import { useAuthStore } from '../../features/auth/authStore'
import { clientPath, settingsPath } from '../paths'

export function ClientGuard() {
  const accessToken = useAuthStore((state) => state.accessToken)
  const tenantId = useAuthStore((state) => state.tenantId)
  const params = useParams()

  if (!accessToken || !tenantId) {
    return <Navigate to={settingsPath()} replace />
  }

  if (params.tenantId !== tenantId) {
    return <Navigate to={clientPath(tenantId)} replace />
  }

  return <Outlet />
}
