// docs/api-spec.md 의 필드 이름과 맞춘다

export type Part = 'PM' | 'DE' | 'BE' | 'FE'

export const PARTS: Part[] = ['PM', 'DE', 'BE', 'FE']

export const PART_LABEL: Record<Part, string> = {
  PM: '기획',
  DE: '디자인',
  BE: '백엔드',
  FE: '프론트엔드',
}

export interface ApiResponse<T> {
  success: true
  status: number
  data: T
  timestamp: string
}

export interface ErrorResponse {
  success: false
  status: number
  code: string
  message: string
  method: string
  path: string
  timestamp: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface Author {
  nickname: string
  generation: number
  part: Part
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
  tokenType: 'Bearer'
}

export interface MyInfo {
  userId: number
  email: string
  name: string
  nickname: string
  generation: number
  part: Part
}
