import axios from 'axios'

export type ApiError = {
  status?: number
  code?: string
  message: string
  path?: string
  traceId?: string
  fieldErrors?: Record<string, string[]>
  details?: unknown
}

const fallbackMessage = 'Request failed'

const readString = (data: Record<string, unknown>, key: string) =>
  typeof data[key] === 'string' ? data[key] : undefined

const readFieldErrors = (data: Record<string, unknown>) => {
  const value = data.fieldErrors ?? data.errors

  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return undefined
  }

  return Object.fromEntries(
    Object.entries(value as Record<string, unknown>).map(([field, messages]) => [
      field,
      Array.isArray(messages)
        ? messages.filter((message): message is string => typeof message === 'string')
        : typeof messages === 'string'
          ? [messages]
          : [],
    ]),
  )
}

export const toApiError = (error: unknown): ApiError => {
  if (!axios.isAxiosError(error)) {
    return {
      message: error instanceof Error ? error.message : fallbackMessage,
    }
  }

  const responseData = error.response?.data

  if (responseData && typeof responseData === 'object') {
    const data = responseData as Record<string, unknown>

    return {
      status: error.response?.status,
      code: readString(data, 'code'),
      message: readString(data, 'message') ?? fallbackMessage,
      path: readString(data, 'path'),
      traceId: readString(data, 'traceId'),
      fieldErrors: readFieldErrors(data),
      details: data,
    }
  }

  return {
    status: error.response?.status,
    message: error.message || fallbackMessage,
  }
}

export const isUnauthorized = (error: ApiError | null | undefined) => error?.status === 401

export const isForbidden = (error: ApiError | null | undefined) => error?.status === 403

export const isValidationError = (error: ApiError | null | undefined) => error?.status === 400

export const isConflict = (error: ApiError | null | undefined) => error?.status === 409

export const displayApiError = (error: ApiError | null | undefined) => {
  if (!error) {
    return ''
  }

  if (isUnauthorized(error)) {
    return 'Session is missing or expired'
  }

  if (isForbidden(error)) {
    return 'You do not have permission for this action'
  }

  return error.message
}
