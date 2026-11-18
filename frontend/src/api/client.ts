import axios from 'axios'
import { tokenStorage } from '../auth/tokenStorage'
import { ERROR_CODE_UNAUTHORIZED, type ErrorResponse } from './types'

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
})

// 요청마다 저장된 토큰을 Authorization 헤더에 담는다.
apiClient.interceptors.request.use((config) => {
  const token = tokenStorage.get()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

type UnauthorizedHandler = () => void
let unauthorizedHandler: UnauthorizedHandler | null = null

// AuthProvider가 등록한다. 토큰이 만료·무효가 되어 401이 오면 로그인 상태를 정리하고 로그인 화면으로 보낸다.
export function setUnauthorizedHandler(handler: UnauthorizedHandler | null): void {
  unauthorizedHandler = handler
}

apiClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    // 로그인 실패(USER-002)도 401이라서 상태 코드가 아니라 code로 구분한다.
    if (
      axios.isAxiosError<ErrorResponse>(error) &&
      error.response?.status === 401 &&
      error.response.data?.code === ERROR_CODE_UNAUTHORIZED
    ) {
      unauthorizedHandler?.()
    }
    return Promise.reject(error)
  },
)
