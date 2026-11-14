import { randomUUID } from 'node:crypto'
import { expect, type APIRequestContext, type Page } from '@playwright/test'

// 백엔드 주소. 다른 포트로 띄웠다면 E2E_API_URL로 바꿉니다.
export const API_URL = process.env.E2E_API_URL ?? 'http://localhost:8081'

export type Part = 'PM' | 'DE' | 'BE' | 'FE'

export interface TestUser {
  email: string
  password: string
  name: string
  nickname: string
  generation: number
  part: Part
}

// 테스트마다 새 계정을 만듭니다. 비밀번호는 실행할 때 무작위로 만들고 코드에 적지 않습니다.
export function newUser(part: Part = 'BE', generation = 14): TestUser {
  const id = randomUUID().replaceAll('-', '')
  return {
    email: `e2e-${id}@example.com`,
    password: randomUUID(),
    name: 'E2E 사용자',
    // 닉네임은 2~10자
    nickname: `e2e${id.slice(0, 6)}`,
    generation,
    part,
  }
}

// 다른 테스트와 섞이지 않게 글 제목, 검색어에 붙이는 고유 값입니다.
export function uniqueTag(): string {
  return randomUUID().slice(0, 8)
}

export function bearer(token: string) {
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
  return body.data.accessToken as string
}

// 가입하고 토큰까지 받습니다. 화면 테스트의 사전 데이터를 만들 때 씁니다.
export async function createUserWithToken(
  request: APIRequestContext,
  part: Part = 'BE',
): Promise<{ user: TestUser; token: string }> {
  const user = newUser(part)
  await signupViaApi(request, user)
  return { user, token: await loginViaApi(request, user) }
}

export async function createPostViaApi(
  request: APIRequestContext,
  token: string,
  post: { title: string; content: string },
): Promise<number> {
  const response = await request.post(`${API_URL}/api/study-posts`, { data: post, headers: bearer(token) })
  expect(response.status()).toBe(201)
  const body = await response.json()
  return body.data.postId as number
}

export async function closePostViaApi(request: APIRequestContext, token: string, postId: number): Promise<void> {
  const response = await request.patch(`${API_URL}/api/study-posts/${postId}`, {
    data: { studyStatus: 'CLOSED' },
    headers: bearer(token),
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
    data: { content },
    headers: bearer(token),
  })
  expect(response.status()).toBe(201)
  const body = await response.json()
  return body.data.commentId as number
}

// 화면에서 로그인하고, 헤더에 닉네임이 보일 때까지 기다립니다.
export async function loginViaUi(page: Page, user: TestUser): Promise<void> {
  await page.goto('/login')
  await page.getByLabel('이메일').fill(user.email)
  await page.getByLabel('비밀번호').fill(user.password)
  await page.getByRole('button', { name: '로그인', exact: true }).click()
  await expect(page).toHaveURL(/\/studies$/)
  await expect(page.getByText(`${user.nickname}님`)).toBeVisible()
}

// 실패 응답이 명세의 ErrorResponse 형식과 code를 따르는지 확인합니다.
export async function expectError(
  response: Awaited<ReturnType<APIRequestContext['get']>>,
  status: number,
  code: string,
): Promise<void> {
  expect(response.status()).toBe(status)
  const body = (await response.json()) as { message?: unknown }
  expect(body).toMatchObject({ success: false, status, code })
  expect(typeof body.message).toBe('string')
}
