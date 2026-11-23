import axios from 'axios'
import { tokenStorage } from '../auth/tokenStorage'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
})

let unauthorizedHandler: (() => void) | null = null

// AuthProvider가 "토큰이 무효일 때 할 일"(상태 초기화 + /login 이동)을 등록해요.
export function setUnauthorizedHandler(handler: (() => void) | null) {
  unauthorizedHandler = handler
}

apiClient.interceptors.request.use((config) => {
  const token = tokenStorage.get()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    // 토큰을 실어 보낸 요청이 401이면 토큰이 만료됐거나 로그아웃된 것이라 로그인 화면으로 보내요.
    // 로그인 실패(401 USER-002)는 토큰 없이 보낸 요청이라 여기서 걸리지 않고 화면에 메시지가 떠요.
    if (
      axios.isAxiosError(error) &&
      error.response?.status === 401 &&
      error.config?.headers?.Authorization
    ) {
      tokenStorage.clear()
      unauthorizedHandler?.()
    }
    return Promise.reject(error)
  },
)
