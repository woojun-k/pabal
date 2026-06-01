import type { UUID } from '../types/api'

const uuidPattern =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

export const createUuid = (): UUID => crypto.randomUUID()

export const isUuid = (value: string): value is UUID => uuidPattern.test(value)
