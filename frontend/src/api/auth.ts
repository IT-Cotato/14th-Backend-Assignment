import { apiClient } from './client'
import type {
  ApiResponse,
  LoginRequest,
  LoginResponse,
  SignupRequest,
  SignupResponse,
  UserMe,
} from './types'

export async function signup(body: SignupRequest): Promise<SignupResponse> {
  const { data } = await apiClient.post<ApiResponse<SignupResponse>>('/api/auth/signup', body)
  return data.data
}

export async function login(body: LoginRequest): Promise<LoginResponse> {
  const { data } = await apiClient.post<ApiResponse<LoginResponse>>('/api/auth/login', body)
  return data.data
}

// 204 응답이라 본문이 없다.
export async function logout(): Promise<void> {
  await apiClient.post('/api/auth/logout')
}

export async function fetchMe(): Promise<UserMe> {
  const { data } = await apiClient.get<ApiResponse<UserMe>>('/api/users/me')
  return data.data
}
