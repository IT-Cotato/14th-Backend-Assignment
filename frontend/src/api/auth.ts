import { client } from './client'
import type { ApiResponse, MyInfo, Part } from './types'

export interface SignupRequest {
  email: string
  password: string
  name: string
  nickname: string
  generation: number
  part: Part
}

export async function signup(body: SignupRequest) {
  const res = await client.post<ApiResponse<{ userId: number }>>('/api/auth/signup', body)
  return res.data.data
}

export async function login(email: string, password: string) {
  const res = await client.post<ApiResponse<{ accessToken: string; tokenType: string }>>(
    '/api/auth/login',
    { email, password },
  )
  return res.data.data
}

export async function logout() {
  await client.post('/api/auth/logout')
}

export async function getMyInfo() {
  const res = await client.get<ApiResponse<MyInfo>>('/api/users/me')
  return res.data.data
}
