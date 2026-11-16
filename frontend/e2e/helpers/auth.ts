import { expect } from '@playwright/test'
import type { APIRequestContext, APIResponse, Page } from '@playwright/test'
import type { LoginResponse, SignupResponse } from '../../src/api/auth'
import type { ApiResponse, ErrorResponse } from '../../src/api/client'
import { createTestUser } from './user'
import type { TestUser } from './user'

// 백엔드 주소. src/api/client.ts의 baseURL과 같다.
export const API_URL = process.env.E2E_API_URL ?? 'http://localhost:8080'

// src/auth/token.ts의 TOKEN_KEY와 같다
const TOKEN_KEY = 'accessToken'

export function authHeaders(token: string): Record<string, string> {
  return { Authorization: `Bearer ${token}` }
}

// 브라우저에 토큰을 미리 넣어 UI 로그인 없이 로그인 상태로 시작한다. (page.goto 전에 호출)
export async function loginInBrowser(page: Page, token: string): Promise<void> {
  await page.addInitScript(
    ([key, value]) => window.localStorage.setItem(key, value),
    [TOKEN_KEY, token] as const,
  )
}

// 응답 코드를 확인하고 ApiResponse의 data를 꺼낸다. 다르면 응답 본문을 담아 실패한다.
export async function readData<T>(response: APIResponse, expectedStatus: number, label: string): Promise<T> {
  if (response.status() !== expectedStatus) {
    throw new Error(
      `${label} 실패: 기대 ${expectedStatus}, 실제 ${response.status()}\n${await response.text()}`,
    )
  }
  const body = (await response.json()) as ApiResponse<T>
  return body.data
}

// 실패 응답의 HTTP 상태와 ErrorResponse.code를 확인한다.
export async function expectApiError(
  response: APIResponse,
  status: number,
  code: string,
): Promise<void> {
  expect(response.status(), await response.text()).toBe(status)
  const body = (await response.json()) as ErrorResponse
  expect(body.code).toBe(code)
}

// API로 회원가입하고 userId를 반환한다.
export async function signupViaApi(request: APIRequestContext, user: TestUser): Promise<number> {
  const response = await request.post(`${API_URL}/api/auth/signup`, { data: user })
  const data = await readData<SignupResponse>(response, 201, '회원가입')
  return data.userId
}

// API로 로그인하고 accessToken을 반환한다.
export async function loginViaApi(request: APIRequestContext, user: TestUser): Promise<string> {
  const response = await request.post(`${API_URL}/api/auth/login`, {
    data: { email: user.email, password: user.password },
  })
  const data = await readData<LoginResponse>(response, 200, '로그인')
  return data.accessToken
}

// 새 사용자를 만들어 가입, 로그인까지 한 번에 한다. (테스트 준비용)
export async function createUserWithToken(
  request: APIRequestContext,
  overrides: Partial<TestUser> = {},
): Promise<{ user: TestUser; token: string }> {
  const user = createTestUser(overrides)
  await signupViaApi(request, user)
  const token = await loginViaApi(request, user)
  return { user, token }
}
