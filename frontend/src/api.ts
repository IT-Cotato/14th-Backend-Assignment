export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message)
  }
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = sessionStorage.getItem('accessToken')
  const headers = new Headers(options.headers)
  if (options.body) headers.set('Content-Type', 'application/json')
  if (token && !['/auth/login', '/auth/signup'].includes(path)) headers.set('Authorization', `Bearer ${token}`)
  const response = await fetch(`${import.meta.env.VITE_API_URL ?? 'http://localhost:8080'}/api${path}`, { ...options, headers })
  if (response.status === 204) return undefined as T
  const body = await response.json()
  if (!response.ok) {
    if (response.status === 401 && headers.has('Authorization') &&
        sessionStorage.getItem('accessToken') === token) {
      sessionStorage.removeItem('accessToken')
      window.dispatchEvent(new Event('unauthorized'))
    }
    throw new ApiError(response.status, body.message ?? '요청을 처리하지 못했어요.')
  }
  return body.data as T
}

export function json(method: string, body: unknown): RequestInit {
  return { method, body: JSON.stringify(body) }
}

export type Part = 'PM' | 'DE' | 'BE' | 'FE'
export const parts = ['PM', 'DE', 'BE', 'FE'] as const
export type Profile = { userId: number; email: string; name: string; nickname: string; generation: number; part: Part }

export function errorMessage(error: unknown) {
  return error instanceof ApiError ? error.message : '서버에 연결하지 못했어요. 잠시 후 다시 시도해 주세요.'
}
