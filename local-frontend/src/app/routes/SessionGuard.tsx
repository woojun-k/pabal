import { Navigate, Outlet } from 'react-router'
import { useAuthStore } from '../../features/auth/authStore'
import { settingsPath } from '../paths'

export function SessionGuard() {
  const accessToken = useAuthStore((state) => state.accessToken)

  if (!accessToken) {
    return <Navigate to={settingsPath()} replace />
  }

  return <Outlet />
}
