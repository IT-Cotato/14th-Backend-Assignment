import { api } from './client'
import type { ApiResponse, LoginRequest, LoginResponse, MyInfo, SignupRequest } from './types'

export async function signup(request: SignupRequest) {
  const { data } = await api.post<ApiResponse<{ userId: number }>>('/api/auth/signup', request)
  return data.data
}

export async function login(request: LoginRequest) {
  const { data } = await api.post<ApiResponse<LoginResponse>>('/api/auth/login', request)
  return data.data
}

export async function logout() {
  await api.post('/api/auth/logout')
}

export async function getMyInfo() {
  const { data } = await api.get<ApiResponse<MyInfo>>('/api/users/me')
  return data.data
}
