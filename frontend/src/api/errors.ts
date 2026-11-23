import axios from 'axios'
import type { ErrorResponse } from './types'

// 서버가 내려준 ErrorResponse의 message를 그대로 돌려줍니다.
// 서버에 닿지 못했거나 형식이 다르면 기본 문구를 씁니다.
export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ErrorResponse>(error)) {
    const message = error.response?.data?.message
    if (message) {
      return message
    }
    if (!error.response) {
      return '서버에 연결할 수 없어요. 잠시 후 다시 시도해 주세요.'
    }
  }
  return '요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요.'
}
