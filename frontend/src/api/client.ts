import axios from 'axios'
import { clearToken, getToken } from '../auth/token'

export interface ApiResponse<T> {
  success: boolean
  status: number
  data: T
  timestamp: string
}

export interface ErrorResponse {
  success: false
  status: number
  code: string
  message: string
  method: string
  path: string
  timestamp: string
}

// 401이어도 /login으로 보내지 않는 요청 (로그인 실패도 401이라 화면에 에러를 보여줘야 한다)
const SKIP_UNAUTHORIZED_REDIRECT = ['/api/auth/login']

let unauthorizedHandler: (() => void) | null = null

// AuthProvider가 등록한다. 사용자 상태 초기화 + /login 이동
export function setUnauthorizedHandler(handler: (() => void) | null): void {
  unauthorizedHandler = handler
}

export const api = axios.create({
  baseURL: 'http://localhost:8080',
})

api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const url: string = error.config?.url ?? ''
    if (
      error.response?.status === 401 &&
      !SKIP_UNAUTHORIZED_REDIRECT.some((path) => url.startsWith(path))
    ) {
      clearToken()
      if (unauthorizedHandler) {
        unauthorizedHandler()
      } else if (window.location.pathname !== '/login') {
        window.location.assign('/login')
      }
    }
    return Promise.reject(error)
  },
)

export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ErrorResponse>(error)) {
    const message = error.response?.data?.message
    if (message) return message
    if (!error.response) return '서버에 연결할 수 없습니다.'
  }
  return '알 수 없는 오류가 발생했습니다.'
}
