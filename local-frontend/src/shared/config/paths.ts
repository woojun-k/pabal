export const localBackendOrigin = 'http://localhost:8080'

export const appPaths = {
  apiRoot: '/api',
  apiPrefix: '/api/v1',
  actuatorPrefix: '/actuator',
  devPrefix: '/dev',
  devToken: '/dev/token',
  health: '/actuator/health',
  websocket: '/websocket',
} as const

export const devServerProxyPrefixes = [
  appPaths.actuatorPrefix,
  appPaths.apiRoot,
  appPaths.devPrefix,
  appPaths.websocket,
] as const

export const urlParseOrigin = 'http://localhost'

export const trimTrailingSlash = (value: string) => value.replace(/\/+$/, '')

export const isAbsoluteUrl = (value: string) => /^[a-z][a-z\d+\-.]*:\/\//i.test(value)

const browserOrigin = () => {
  const global = globalThis as typeof globalThis & {
    window?: {
      location?: {
        origin?: string
      }
    }
  }

  return global.window?.location?.origin
}

export const toBrowserAbsoluteUrl = (value: string) => {
  const origin = browserOrigin()

  if (isAbsoluteUrl(value) || !origin) {
    return value
  }

  return trimTrailingSlash(new URL(value, origin).toString())
}

export const pathnameOf = (value: string) => new URL(value, urlParseOrigin).pathname
