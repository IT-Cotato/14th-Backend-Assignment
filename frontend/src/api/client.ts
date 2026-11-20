import axios from 'axios'
import type { ErrorResponse } from '../types'

const TOKEN_KEY = 'accessToken'
const LOGIN_PATH = '/api/auth/login'

/** 401 을 받았을 때 화면 쪽(AuthProvider)에 알리는 이벤트 이름 */
export const UNAUTHORIZED_EVENT = 'auth:unauthorized'

export const tokenStorage = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

export const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
})

client.interceptors.request.use((config) => {
  const token = tokenStorage.get()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  // 공통 응답({ success, status, data, timestamp })에서 data 만 꺼내 돌려준다
  (response) => {
    const body: unknown = response.data
    if (body && typeof body === 'object' && 'data' in body) {
      response.data = body.data
    }
    return response
  },
  (error: unknown) => {
    // 로그인 요청의 401 은 "비밀번호가 틀림" 이므로 화면에서 메시지로 보여준다
    if (
      axios.isAxiosError(error) &&
      error.response?.status === 401 &&
      error.config?.url !== LOGIN_PATH
    ) {
      tokenStorage.clear()
      window.dispatchEvent(new Event(UNAUTHORIZED_EVENT))
    }
    return Promise.reject(error)
  },
)

const DEFAULT_ERROR_MESSAGE = '요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.'

/** 서버가 내려준 에러 메시지를 꺼낸다. 없으면 기본 문구를 쓴다. */
export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ErrorResponse>(error)) {
    const message = error.response?.data?.message
    if (message) {
      return message
    }
    if (!error.response) {
      return '서버에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.'
    }
  }
  return DEFAULT_ERROR_MESSAGE
}
