import { request } from '@playwright/test'
import { API_URL } from './support/env.ts'

// webServer는 프론트만 띄워요. 백엔드가 꺼져 있으면 테스트마다 알 수 없는 실패가 나니까 시작 전에 확인해요
export default async function globalSetup() {
  const context = await request.newContext({ baseURL: API_URL })
  try {
    const res = await context.get('/v3/api-docs', { timeout: 10_000 })
    if (!res.ok()) {
      throw new Error(`백엔드가 ${res.status()}로 응답해요.`)
    }
  } catch (error) {
    throw new Error(
      `백엔드(${API_URL})에 연결할 수 없어요. 백엔드 서버와 DB를 먼저 켜요. (backend/에서 ./gradlew bootRun)\n원인: ${String(error)}`,
      { cause: error },
    )
  } finally {
    await context.dispose()
  }
}
