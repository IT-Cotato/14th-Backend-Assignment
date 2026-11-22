import { apiClient } from './client'
import type {
  ApiResponse,
  LoginRequest,
  LoginResponse,
  SignupRequest,
  SignupResponse,
  User,
} from './types'

export async function signup(request: SignupRequest): Promise<SignupResponse> {
  const res = await apiClient.post<ApiResponse<SignupResponse>>('/api/auth/signup', request)
  return res.data.data
}

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const res = await apiClient.post<ApiResponse<LoginResponse>>('/api/auth/login', request)
  return res.data.data
}

// 204 응답이라 본문이 없어요
export async function logout(): Promise<void> {
  await apiClient.post('/api/auth/logout')
}

export async function getMe(): Promise<User> {
  const res = await apiClient.get<ApiResponse<User>>('/api/users/me')
  return res.data.data
}
