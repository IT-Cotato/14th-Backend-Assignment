import { randomUUID } from 'node:crypto'
import { expect, type APIRequestContext, type Page } from '@playwright/test'

export const API_URL = process.env.E2E_API_URL ?? 'http://localhost:8080'

type Part = 'PM' | 'DE' | 'BE' | 'FE'

export interface TestUser {
  email: string
  password: string
  name: string
  nickname: string
  generation: number
  part: Part
}

/** 테스트마다 겹치지 않는 짧은 값 */
export function uniqueId(length = 6) {
  return randomUUID().replace(/-/g, '').slice(0, length)
}

/** 비밀번호는 코드에 적지 않는다. E2E_PASSWORD 가 없으면 실행할 때마다 새로 만든다. */
const generatedPassword = `pw-${randomUUID()}`
export function testPassword() {
  return process.env.E2E_PASSWORD ?? generatedPassword
}

/** e2e- 로 시작하는 이메일로 개발 DB의 직접 만든 데이터와 구분한다. */
export function newUser(overrides: Partial<TestUser> = {}): TestUser {
  const id = uniqueId()
  return {
    email: `e2e-${id}-${uniqueId(4)}@cotato.test`,
    password: testPassword(),
    name: '테스트',
    nickname: `e2e${id}`,
    generation: 14,
    part: 'BE',
    ...overrides,
  }
}

export async function signupViaApi(request: APIRequestContext, overrides: Partial<TestUser> = {}) {
  const user = newUser(overrides)
  const response = await request.post(`${API_URL}/api/auth/signup`, { data: user })
  expect(response.status(), await response.text()).toBe(201)
  return user
}

export async function loginViaApi(request: APIRequestContext, user: TestUser): Promise<string> {
  const response = await request.post(`${API_URL}/api/auth/login`, {
    data: { email: user.email, password: user.password },
  })
  expect(response.status()).toBe(200)
  return (await response.json()).data.accessToken
}

export function authHeader(token: string) {
  return { Authorization: `Bearer ${token}` }
}

/** 회원가입 후 토큰까지 받아 둔다. */
export async function createLoggedInUser(request: APIRequestContext, overrides: Partial<TestUser> = {}) {
  const user = await signupViaApi(request, overrides)
  const token = await loginViaApi(request, user)
  return { user, token }
}

export async function createPostViaApi(
  request: APIRequestContext,
  token: string,
  post: { title: string; content: string },
): Promise<number> {
  const response = await request.post(`${API_URL}/api/study-posts`, { data: post, headers: authHeader(token) })
  expect(response.status()).toBe(201)
  return (await response.json()).data.postId
}

export async function closePostViaApi(request: APIRequestContext, token: string, postId: number) {
  const response = await request.patch(`${API_URL}/api/study-posts/${postId}`, {
    data: { studyStatus: 'CLOSED' },
    headers: authHeader(token),
  })
  expect(response.status()).toBe(200)
}

export async function createCommentViaApi(request: APIRequestContext, token: string, postId: number, content: string) {
  const response = await request.post(`${API_URL}/api/study-posts/${postId}/comments`, {
    data: { content },
    headers: authHeader(token),
  })
  expect(response.status()).toBe(201)
  return (await response.json()).data.commentId as number
}

/** 로그인 화면을 거쳐 로그인한다. */
export async function loginViaUi(page: Page, user: TestUser) {
  await page.goto('/login')
  await page.getByLabel('이메일').fill(user.email)
  await page.getByLabel('비밀번호').fill(user.password)
  await page.getByRole('button', { name: '로그인' }).click()
  await expect(page).toHaveURL(/\/studies$/)
  await expect(page.getByText(`${user.nickname}님`)).toBeVisible()
}
