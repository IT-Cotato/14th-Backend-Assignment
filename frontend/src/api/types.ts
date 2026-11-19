// 백엔드 API 명세(docs/api-spec.md)와 맞춘 타입
// tsconfig의 erasableSyntaxOnly 때문에 enum 대신 문자열 유니온을 써요.

export type Part = 'PM' | 'DE' | 'BE' | 'FE'

export const PART_OPTIONS: { value: Part; label: string }[] = [
  { value: 'PM', label: 'PM (기획)' },
  { value: 'DE', label: 'DE (디자인)' },
  { value: 'BE', label: 'BE (백엔드)' },
  { value: 'FE', label: 'FE (프론트엔드)' },
]

export function partLabel(part: Part): string {
  return PART_OPTIONS.find((option) => option.value === part)?.label ?? part
}

// 공통 성공 응답. 각 API 함수는 data만 꺼내서 돌려줘요.
export interface ApiResponse<T> {
  success: boolean
  status: number
  data: T
  timestamp: string
}

// 공통 실패 응답
export interface ErrorResponse {
  success: false
  status: number
  code: string
  message: string
  method: string
  path: string
  timestamp: string
}

// 목록 API 공통 페이지 응답. page는 0부터 시작해요.
export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

// 모집글·댓글의 작성자 정보
export interface Author {
  nickname: string
  generation: number
  part: Part
}

// ---------- 인증 ----------

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
