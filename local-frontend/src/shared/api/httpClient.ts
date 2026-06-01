import axios from 'axios'
import { env } from '../config/env'
import { appPaths, pathnameOf } from '../config/paths'
import { createAuthHeaders } from '../security/session'

export const httpClient = axios.create({
  baseURL: env.apiBaseUrl,
  timeout: 15_000,
  headers: {
    Accept: 'application/json',
    'Content-Type': 'application/json',
  },
})

const isUnauthenticatedRequest = (url?: string) => {
  if (!url) {
    return false
  }

  return pathnameOf(url) === appPaths.devToken
}

httpClient.interceptors.request.use((config) => {
  if (!isUnauthenticatedRequest(config.url)) {
    Object.assign(config.headers, createAuthHeaders())
  }

  return config
})
