import { randomUUID } from 'node:crypto'
import { expect, type APIRequestContext, type Page } from '@playwright/test'

/** 화면을 거치지 않고 서버를 직접 호출할 때 쓰는 백엔드 주소 */
export const API_URL = process.env.E2E_API_URL ?? 'http://localhost:8080'

export type Part = 'PM' | 'DE' | 'BE' | 'FE'

export interface TestUser {
  email: string
  password: string
  name: string
  nickname: string
  generation: number
  part: Part
}

/**
 * 테스트마다 새 사용자를 만든다.
 * 이메일은 e2e- 로 시작해서 직접 만든 데이터와 구분되고, 비밀번호는 실행할 때마다 새로 만든다.
 */
export function newUser(overrides: Partial<TestUser> = {}): TestUser {
  const id = randomUUID()
  return {
    email: `e2e-${id}@example.com`,
    password: `pw-${randomUUID()}`,
    name: '테스트',
    nickname: `e2e${id.slice(0, 6)}`,
    generation: 14,
    part: 'BE',
    ...overrides,
  }
}

/** 다른 테스트의 글과 겹치지 않는 제목을 만든다 (50자 이하) */
export function uniqueTitle(prefix: string): string {
  return `${prefix} ${randomUUID().slice(0, 8)}`
}

export function authHeader(token: string): Record<string, string> {
  return { Authorization: `Bearer ${token}` }
}

export async function signupViaApi(request: APIRequestContext, user: TestUser): Promise<void> {
  const response = await request.post(`${API_URL}/api/auth/signup`, { data: user })
  expect(response.status()).toBe(201)
}

export async function loginViaApi(request: APIRequestContext, user: TestUser): Promise<string> {
  const response = await request.post(`${API_URL}/api/auth/login`, {
    data: { email: user.email, password: user.password },
  })
  expect(response.status()).toBe(200)
  const body = await response.json()
  return body.data.accessToken
}

/** 가입과 로그인을 API 로 끝내고 토큰을 돌려준다 */
export async function createUserViaApi(
  request: APIRequestContext,
  overrides: Partial<TestUser> = {},
): Promise<{ user: TestUser; token: string }> {
  const user = newUser(overrides)
  await signupViaApi(request, user)
  const token = await loginViaApi(request, user)
  return { user, token }
}

export async function createPostViaApi(
  request: APIRequestContext,
  token: string,
  post: { title: string; content: string },
): Promise<number> {
  const response = await request.post(`${API_URL}/api/study-posts`, {
    headers: authHeader(token),
    data: post,
  })
  expect(response.status()).toBe(201)
  const body = await response.json()
  return body.data.postId
}

export async function closePostViaApi(
  request: APIRequestContext,
  token: string,
  postId: number,
): Promise<void> {
  const response = await request.patch(`${API_URL}/api/study-posts/${postId}`, {
    headers: authHeader(token),
    data: { studyStatus: 'CLOSED' },
  })
  expect(response.status()).toBe(200)
}

export async function createCommentViaApi(
  request: APIRequestContext,
  token: string,
  postId: number,
  content: string,
): Promise<number> {
  const response = await request.post(`${API_URL}/api/study-posts/${postId}/comments`, {
    headers: authHeader(token),
    data: { content },
  })
  expect(response.status()).toBe(201)
  const body = await response.json()
  return body.data.commentId
}

/** 로그인 화면에서 직접 입력해 로그인한다 */
export async function loginViaUi(page: Page, user: TestUser): Promise<void> {
  await page.goto('/login')
  await page.getByLabel('이메일', { exact: true }).fill(user.email)
  await page.getByLabel('비밀번호', { exact: true }).fill(user.password)
  await page.getByRole('button', { name: '로그인', exact: true }).click()
  await expect(page).toHaveURL(/\/studies$/)
  await expect(page.getByText(`${user.nickname}님`)).toBeVisible()
}

/** 다음에 뜨는 확인 창(confirm)에서 "확인"을 누른다 */
export function acceptNextDialog(page: Page): void {
  page.once('dialog', (dialog) => void dialog.accept())
}
