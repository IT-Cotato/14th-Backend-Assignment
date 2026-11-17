import axios, { isAxiosError } from 'axios'
import type { ErrorResponse } from './types.ts'
import { clearToken, getToken } from './token.ts'

export const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
})

// 요청마다 저장된 토큰을 Authorization 헤더에 붙인다
client.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 토큰을 보냈는데 401이 오면 만료되었거나 무효가 된 것이므로 로그인 화면으로 보낸다.
// (토큰 없이 보낸 로그인 요청의 401은 "비밀번호가 틀림"이므로 건드리지 않는다)
client.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (isAxiosError(error) && error.response?.status === 401 && getToken()) {
      clearToken()
      if (window.location.pathname !== '/login') {
        window.location.assign('/login')
      }
    }
    return Promise.reject(error)
  },
)

// 서버가 내려준 에러 메시지를 꺼낸다
export function getErrorMessage(error: unknown): string {
  if (isAxiosError<ErrorResponse>(error)) {
    if (error.response?.data?.message) {
      return error.response.data.message
    }
    if (!error.response) {
      return '서버에 연결할 수 없어요. 잠시 후 다시 시도해 주세요.'
    }
  }
  return '알 수 없는 오류가 발생했어요.'
}
