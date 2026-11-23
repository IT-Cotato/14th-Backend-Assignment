import { request } from './client'

export const parts = ['PM', 'DE', 'BE', 'FE'] as const
export type Part = (typeof parts)[number]

type Credentials = { email: string; password: string }
type SignupRequest = Credentials & {
  name: string
  nickname: string
  generation: number
  part: Part
}

export type Profile = Omit<SignupRequest, 'password'> & { userId: number }

export function signup(body: SignupRequest) {
  return request<{ userId: number }>('/api/auth/signup', { method: 'POST', body })
}

export function login(body: Credentials) {
  return request<{ accessToken: string; tokenType: 'Bearer' }>('/api/auth/login', { method: 'POST', body })
}

export function logout(token: string) {
  return request<void>('/api/auth/logout', { method: 'POST', token })
}

export function getProfile(token: string, signal: AbortSignal) {
  return request<Profile>('/api/users/me', { token, signal })
}
