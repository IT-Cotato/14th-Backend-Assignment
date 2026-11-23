import { client } from './client.ts'
import type { ApiResponse, LoginRequest, LoginResponse, SignupRequest, User } from './types.ts'

export async function signup(body: SignupRequest) {
  const res = await client.post<ApiResponse<{ userId: number }>>('/auth/signup', body)
  return res.data.data
}

export async function login(body: LoginRequest) {
  const res = await client.post<ApiResponse<LoginResponse>>('/auth/login', body)
  return res.data.data
}

export async function logout() {
  await client.post('/auth/logout')
}

export async function getMe() {
  const res = await client.get<ApiResponse<User>>('/users/me')
  return res.data.data
}
