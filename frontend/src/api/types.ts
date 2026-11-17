export type Part = 'PM' | 'DE' | 'BE' | 'FE'

export const PARTS: { value: Part; label: string }[] = [
  { value: 'PM', label: '기획 (PM)' },
  { value: 'DE', label: '디자인 (DE)' },
  { value: 'BE', label: '백엔드 (BE)' },
  { value: 'FE', label: '프론트엔드 (FE)' },
]

export function getPartLabel(part: Part): string {
  return PARTS.find((p) => p.value === part)?.label ?? part
}

// 인증이 필요한 요청에서 토큰이 없거나 유효하지 않거나 로그아웃된 경우의 응답 code
export const ERROR_CODE_UNAUTHORIZED = 'COMMON-004'

// 성공 응답은 data 안쪽만 필요해서 axios 응답 래퍼 타입으로만 사용한다.
export interface ApiResponse<T> {
  success: boolean
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

export interface SignupRequest {
  email: string
  password: string
  name: string
  nickname: string
  generation: number
  part: Part
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

export interface UserMe {
  userId: number
  email: string
  name: string
  nickname: string
  generation: number
  part: Part
}
