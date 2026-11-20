import type { LoginRequest, SignupRequest, TokenResponse, User } from '../types'
import { client } from './client'

export async function signup(request: SignupRequest): Promise<{ userId: number }> {
  const response = await client.post<{ userId: number }>('/api/auth/signup', request)
  return response.data
}

export async function login(request: LoginRequest): Promise<TokenResponse> {
  const response = await client.post<TokenResponse>('/api/auth/login', request)
  return response.data
}

export async function logout(): Promise<void> {
  await client.post('/api/auth/logout')
}

export async function fetchMe(): Promise<User> {
  const response = await client.get<User>('/api/users/me')
  return response.data
}
