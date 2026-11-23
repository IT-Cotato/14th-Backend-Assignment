import axios from 'axios'
import { tokenStorage } from '../auth/tokenStorage'

// 백엔드 API 클라이언트입니다. 주소는 VITE_API_BASE_URL로 바꿀 수 있습니다.
export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8081',
  headers: { 'Content-Type': 'application/json' },
})

// 로그인과 회원가입의 401은 "이메일 또는 비밀번호가 틀려요" 같은 입력 오류라서,
// 토큰을 지우고 이동하지 않고 화면에 메시지를 보여줍니다.
const AUTH_FORM_PATHS = ['/api/auth/login', '/api/auth/signup']

let unauthorizedHandler: (() => void) | null = null

// AuthProvider가 401을 받았을 때 할 일(상태 초기화, /login 이동)을 등록합니다.
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
  (error) => {
    const isUnauthorized = axios.isAxiosError(error) && error.response?.status === 401
    const isAuthForm = AUTH_FORM_PATHS.some((path) => error.config?.url?.endsWith(path))

    if (isUnauthorized && !isAuthForm) {
      tokenStorage.clear()
      unauthorizedHandler?.()
    }
    return Promise.reject(error)
  },
)
