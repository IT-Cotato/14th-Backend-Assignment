// 백엔드 공통 응답 (docs/api-spec.md)
export interface ApiResponse<T> {
  success: boolean
  status: number
  data: T
}

export interface ErrorResponse {
  success: false
  status: number
  code: string
  message: string
}

export type Part = 'PM' | 'DE' | 'BE' | 'FE'

export const PART_LABELS: Record<Part, string> = {
  PM: '기획',
  DE: '디자인',
  BE: '백엔드',
  FE: '프론트엔드',
}

export interface SignupRequest {
  email: string
  password: string
  name: string
  nickname: string
  generation: number
  part: Part
}

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
}

export interface User {
  userId: number
  email: string
  name: string
  nickname: string
  generation: number
  part: Part
}
