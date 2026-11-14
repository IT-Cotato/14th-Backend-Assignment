import { apiClient } from './client'
import type {
  ApiResponse,
  LoginRequest,
  LoginResponse,
  MyInfoResponse,
  SignupRequest,
  SignupResponse,
} from './types'

// 각 함수는 공통 응답 래퍼를 벗기고 data만 돌려줍니다.

export async function signup(request: SignupRequest): Promise<SignupResponse> {
  const { data } = await apiClient.post<ApiResponse<SignupResponse>>('/api/auth/signup', request)
  return data.data
}

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const { data } = await apiClient.post<ApiResponse<LoginResponse>>('/api/auth/login', request)
  return data.data
}

// 204 본문 없음
export async function logout(): Promise<void> {
  await apiClient.post('/api/auth/logout')
}

export async function getMe(): Promise<MyInfoResponse> {
  const { data } = await apiClient.get<ApiResponse<MyInfoResponse>>('/api/users/me')
  return data.data
}
