import { Navigate } from 'react-router'
import { useAuthStore } from '../../features/auth/authStore'
import { clientPath, settingsPath } from '../paths'

export function RootRedirect() {
  const accessToken = useAuthStore((state) => state.accessToken)
  const tenantId = useAuthStore((state) => state.tenantId)

  if (!accessToken || !tenantId) {
    return <Navigate to={settingsPath()} replace />
  }

  return <Navigate to={clientPath(tenantId)} replace />
}
