import axios from 'axios'
import { getToken } from '../auth/token'

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let onUnauthorized: (() => void) | null = null

/** 토큰이 만료되거나 무효가 되어 401이 왔을 때 실행할 동작을 등록한다. */
export function setUnauthorizedHandler(handler: (() => void) | null) {
  onUnauthorized = handler
}

api.interceptors.response.use(
  (response) => response,
  (error) => {
    // 토큰을 들고 보낸 요청이 401이면 토큰이 무효라는 뜻이다. (로그인 실패 401은 토큰이 없어서 제외된다)
    if (axios.isAxiosError(error) && error.response?.status === 401 && error.config?.headers?.Authorization) {
      onUnauthorized?.()
    }
    return Promise.reject(error)
  },
)
