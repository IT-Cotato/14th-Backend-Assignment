import axios from 'axios'

import type { ErrorResponse } from './types'

const DEFAULT_MESSAGE = '요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요.'

// 서버가 내려준 ErrorResponse의 message를 꺼내요. 없으면 기본 문구를 써요.
export function getErrorMessage(error: unknown, fallback: string = DEFAULT_MESSAGE): string {
  if (axios.isAxiosError<ErrorResponse>(error)) {
    if (!error.response) {
      return '서버에 연결할 수 없어요. 백엔드 서버가 켜져 있는지 확인해 주세요.'
    }
    const message = error.response.data?.message
    if (message) {
      return message
    }
  }
  return fallback
}
