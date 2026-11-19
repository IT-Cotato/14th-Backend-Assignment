import { randomBytes, randomUUID } from 'node:crypto'

import { expect, type APIRequestContext, type Page } from '@playwright/test'

// E2E 공통 함수
// - 테스트마다 새 계정·글·댓글을 만들어서 테스트끼리 서로 기대지 않아요.
// - 이메일은 e2e- 로 시작해서 직접 만든 데이터와 구분돼요.
// - 비밀번호는 코드에 적지 않고 실행할 때마다 무작위로 만들어요.

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

// 짧은 무작위 문자열 (예: 3fa9c1)
export function randomId(): string {
  return randomBytes(3).toString('hex')
}

// 다른 글과 겹치지 않는 검색용 키워드 (예: e2ek3fa9c1)
export function uniqueKeyword(): string {
  return `e2ek${randomId()}`
}

export function newUser(overrides: Partial<TestUser> = {}): TestUser {
  const id = randomId()
  return {
    email: `e2e-${Date.now()}-${id}@test.com`,
    password: randomUUID(),
    name: '테스트',
    // 닉네임은 2~10자라서 e2e + 6자 = 9자로 만들어요
    nickname: `e2e${id}`,
    generation: 14,
    part: 'BE',
    ...overrides,
  }
}

// ---------- API로 준비 데이터 만들기 ----------
// 테스트 주제가 아닌 준비 단계는 API로 빠르게 만들어요.

function authHeader(token: string) {
  return { Authorization: `Bearer ${token}` }
}

export async function signupByApi(request: APIRequestContext, user: TestUser): Promise<void> {
  const response = await request.post(`${API_URL}/api/auth/signup`, { data: user })
  expect(response.ok(), await response.text()).toBeTruthy()
}

export async function loginByApi(request: APIRequestContext, user: TestUser): Promise<string> {
  const response = await request.post(`${API_URL}/api/auth/login`, {
    data: { email: user.email, password: user.password },
  })
  expect(response.ok(), await response.text()).toBeTruthy()
  const body = await response.json()
  return body.data.accessToken
}

// 가입하고 토큰까지 받아요
export async function createUserByApi(
  request: APIRequestContext,
  overrides: Partial<TestUser> = {},
): Promise<{ user: TestUser; token: string }> {
  const user = newUser(overrides)
  await signupByApi(request, user)
  const token = await loginByApi(request, user)
  return { user, token }
}

export async function createPostByApi(
  request: APIRequestContext,
  token: string,
  post: { title: string; content: string },
): Promise<number> {
  const response = await request.post(`${API_URL}/api/study-posts`, {
    headers: authHeader(token),
    data: post,
  })
  expect(response.ok(), await response.text()).toBeTruthy()
  const body = await response.json()
  return body.data.postId
}

export async function closePostByApi(
  request: APIRequestContext,
  token: string,
  postId: number,
): Promise<void> {
  const response = await request.patch(`${API_URL}/api/study-posts/${postId}`, {
    headers: authHeader(token),
    data: { studyStatus: 'CLOSED' },
  })
  expect(response.ok(), await response.text()).toBeTruthy()
}

export async function deletePostByApi(
  request: APIRequestContext,
  token: string,
  postId: number,
): Promise<void> {
  const response = await request.delete(`${API_URL}/api/study-posts/${postId}`, {
    headers: authHeader(token),
  })
  expect(response.status(), await response.text()).toBe(204)
}

export async function createCommentByApi(
  request: APIRequestContext,
  token: string,
  postId: number,
  content: string,
): Promise<number> {
  const response = await request.post(`${API_URL}/api/study-posts/${postId}/comments`, {
    headers: authHeader(token),
    data: { content },
  })
  expect(response.ok(), await response.text()).toBeTruthy()
  const body = await response.json()
  return body.data.commentId
}

// ---------- 화면 조작 ----------

// 헤더의 사용자 메뉴 (로그인하면 닉네임이 보여요)
export function userMenu(page: Page) {
  return page.getByRole('navigation', { name: '사용자 메뉴' })
}

// 로그인은 항상 화면으로 해요
export async function loginByUi(page: Page, user: TestUser): Promise<void> {
  await page.goto('/login')
  await page.getByLabel('이메일').fill(user.email)
  await page.getByLabel('비밀번호').fill(user.password)
  await page.getByRole('button', { name: '로그인' }).click()

  await expect(page).toHaveURL(/\/studies$/)
  await expect(userMenu(page).getByText(user.nickname)).toBeVisible()
}
