const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/+$/, '')

type RequestOptions = {
  method?: 'GET' | 'POST' | 'PATCH' | 'DELETE'
  body?: object
  token?: string
  signal?: AbortSignal
}

export class ApiError extends Error {
  readonly status: number
  readonly code?: string

  constructor(message: string, status: number, code?: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers({ Accept: 'application/json' })
  if (options.body) headers.set('Content-Type', 'application/json')
  if (options.token) headers.set('Authorization', `Bearer ${options.token}`)

  const response = await fetch(`${apiBaseUrl}${path}`, {
    method: options.method ?? 'GET',
    headers,
    body: options.body && JSON.stringify(options.body),
    signal: options.signal,
  })

  if (response.status === 204) return undefined as T

  const payload = await response.json().catch(() => null)
  if (!response.ok || payload?.success === false) {
    throw new ApiError(
      typeof payload?.message === 'string' && payload.message
        ? payload.message
        : '요청을 처리하지 못했습니다. 다시 시도해 주세요.',
      response.status,
      typeof payload?.code === 'string' ? payload.code : undefined,
    )
  }

  if (payload?.success !== true || !('data' in payload)) {
    throw new ApiError('서버 응답을 확인할 수 없습니다.', response.status)
  }
  return payload.data as T
}

export function errorMessage(error: unknown): string {
  return error instanceof ApiError
    ? error.message
    : '서버에 연결할 수 없습니다. 연결을 확인한 뒤 다시 시도해 주세요.'
}
