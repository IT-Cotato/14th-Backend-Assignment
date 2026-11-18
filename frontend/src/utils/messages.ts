import axios from 'axios'
import type { ErrorResponse } from '../api/types'

// 서버가 내려준 에러 메시지를 우선 보여주고, 서버에 닿지 못한 경우는 별도 문구를 보여준다.
export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ErrorResponse>(error)) {
    const serverMessage = error.response?.data?.message
    if (serverMessage) {
      return serverMessage
    }
    if (!error.response) {
      return '서버에 연결할 수 없어요. 잠시 후 다시 시도해 주세요.'
    }
  }
  return '알 수 없는 오류가 발생했어요.'
}

// 다른 화면에서 navigate(..., { state: { message } }) 로 넘겨준 안내 문구를 꺼낸다.
export function getFlashMessage(state: unknown): string | null {
  if (typeof state === 'object' && state !== null && 'message' in state) {
    const { message } = state as { message: unknown }
    return typeof message === 'string' ? message : null
  }
  return null
}
