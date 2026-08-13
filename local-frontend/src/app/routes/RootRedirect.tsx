import { Navigate } from 'react-router'
import { useAuthStore } from '../../features/auth/authStore'
import { roomsPath, settingsPath } from '../paths'

export function RootRedirect() {
  const accessToken = useAuthStore((state) => state.accessToken)

  if (!accessToken) {
    return <Navigate to={settingsPath()} replace />
  }

  return <Navigate to={roomsPath()} replace />
}
