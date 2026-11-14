import axios from 'axios'

// 백엔드 API 클라이언트입니다. 주소는 VITE_API_BASE_URL로 바꿀 수 있습니다.
export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8081',
  headers: { 'Content-Type': 'application/json' },
})
