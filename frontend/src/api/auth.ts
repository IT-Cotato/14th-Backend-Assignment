import { apiClient } from './client'
import type { ApiResponse, LoginRequest, LoginResponse, SignupRequest, User } from './types'

export async function signup(request: SignupRequest): Promise<{ userId: number }> {
  const response = await apiClient.post<ApiResponse<{ userId: number }>>('/api/auth/signup', request)
  return response.data.data
}

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const response = await apiClient.post<ApiResponse<LoginResponse>>('/api/auth/login', request)
  return response.data.data
}

// 204 No Content라서 돌려줄 값이 없어요
export async function logout(): Promise<void> {
  await apiClient.post('/api/auth/logout')
}

export async function getMe(): Promise<User> {
  const response = await apiClient.get<ApiResponse<User>>('/api/users/me')
  return response.data.data
}
