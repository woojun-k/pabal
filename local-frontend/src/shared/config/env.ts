import { appPaths, toBrowserAbsoluteUrl, trimTrailingSlash } from './paths'

export type BackendMode = 'mock' | 'real'

const readEnv = (key: keyof ImportMetaEnv, fallback: string) => {
  const value = import.meta.env[key]
  return trimTrailingSlash(value && value.trim().length > 0 ? value : fallback)
}

const readBackendMode = (): BackendMode => {
  const value = import.meta.env.VITE_BACKEND_MODE

  if (value === 'mock' || value === 'real') {
    return value
  }

  return import.meta.env.DEV ? 'mock' : 'real'
}

export const env = {
  apiBaseUrl: readEnv('VITE_API_BASE_URL', ''),
  backendMode: readBackendMode(),
  wsUrl: toBrowserAbsoluteUrl(readEnv('VITE_WS_URL', appPaths.websocket)),
}
