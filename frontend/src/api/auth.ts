import { api } from './client'
import type { ApiResponse } from './client'

export const PARTS = ['PM', 'DE', 'BE', 'FE'] as const
export type Part = (typeof PARTS)[number]

export interface SignupRequest {
  email: string
  password: string
  name: string
  nickname: string
  generation: number | null
  part: Part | null
}

export interface SignupResponse {
  userId: number
}

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
}

export interface MeResponse {
  userId: number
  email: string
  name: string
  nickname: string
  generation: number
  part: Part
}

export async function signup(request: SignupRequest): Promise<SignupResponse> {
  const res = await api.post<ApiResponse<SignupResponse>>('/api/auth/signup', request)
  return res.data.data
}

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const res = await api.post<ApiResponse<LoginResponse>>('/api/auth/login', request)
  return res.data.data
}

export async function logout(): Promise<void> {
  await api.post('/api/auth/logout')
}

export async function getMe(): Promise<MeResponse> {
  const res = await api.get<ApiResponse<MeResponse>>('/api/users/me')
  return res.data.data
}
