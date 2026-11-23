import axios, { AxiosError } from 'axios'
import type { ErrorResponse } from './types'

const TOKEN_KEY = 'accessToken'

export const tokenStorage = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

export const client = axios.create({
  headers: { 'Content-Type': 'application/json' },
})

client.interceptors.request.use((config) => {
  const token = tokenStorage.get()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 토큰을 보냈는데 401이면(만료·로그아웃된 토큰) 토큰을 지우고 로그인 화면으로 보내요
client.interceptors.response.use(
  (res) => res,
  (error: AxiosError) => {
    const sentToken = Boolean(error.config?.headers?.Authorization)
    if (error.response?.status === 401 && sentToken) {
      tokenStorage.clear()
      if (window.location.pathname !== '/login') {
        window.location.assign('/login')
      }
    }
    return Promise.reject(error)
  },
)

/** 서버 ErrorResponse의 message를 꺼내요 */
export function getErrorMessage(error: unknown, fallback = '요청을 처리하지 못했어요.'): string {
  if (axios.isAxiosError<ErrorResponse>(error)) {
    return error.response?.data?.message ?? fallback
  }
  return fallback
}
