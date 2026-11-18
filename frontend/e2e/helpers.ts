import { randomBytes } from 'node:crypto'
import { expect, type APIRequestContext, type Page } from '@playwright/test'

const API_URL = process.env.E2E_API_URL ?? 'http://localhost:8080/api'

export interface TestUser {
  email: string
  password: string
  name: string
  nickname: string
  generation: number
  part: 'BE'
}

// 테스트마다 새 계정을 만든다. 이메일은 e2e- 로 시작하고, 비밀번호는 실행할 때마다 무작위로 만든다.
export function newUser(): TestUser {
  const id = randomBytes(4).toString('hex')
  return {
    email: `e2e-${id}-${Date.now()}@test.com`,
    password: `${randomBytes(9).toString('base64url')}a1`,
    name: '테스터',
    nickname: `e${id}`,
    generation: 14,
    part: 'BE',
  }
}

// 테스트마다 겹치지 않는 글 제목을 만든다
export function uniqueTitle(prefix: string): string {
  return `${prefix} ${randomBytes(4).toString('hex')}`
}

export async function signupViaApi(request: APIRequestContext, user: TestUser) {
  const res = await request.post(`${API_URL}/auth/signup`, { data: user })
  expect(res.status(), '회원가입 API가 201을 돌려줘야 해요 (백엔드가 켜져 있나요?)').toBe(201)
}

async function loginViaApi(request: APIRequestContext, user: TestUser): Promise<string> {
  const res = await request.post(`${API_URL}/auth/login`, { data: { email: user.email, password: user.password } })
  expect(res.status()).toBe(200)
  return (await res.json()).data.accessToken
}

// 계정을 만들어 두고 화면에서 로그인한다
export async function createUserAndLogin(page: Page, request: APIRequestContext): Promise<TestUser> {
  const user = newUser()
  await signupViaApi(request, user)
  await loginViaUi(page, user)
  return user
}

export async function loginViaUi(page: Page, user: TestUser) {
  await page.goto('/login')
  await page.getByLabel('이메일').fill(user.email)
  await page.getByLabel('비밀번호').fill(user.password)
  await page.getByRole('button', { name: '로그인' }).click()
  await expect(page).toHaveURL('/studies')
  await expect(page.getByText(user.nickname)).toBeVisible()
}

// 화면을 거치지 않고 API로 글을 만든다. (준비 단계용)
export async function createPostViaApi(
  request: APIRequestContext,
  user: TestUser,
  title: string,
  content = '주 2회 온라인으로 진행해요.',
): Promise<number> {
  const token = await loginViaApi(request, user)
  const res = await request.post(`${API_URL}/study-posts`, {
    data: { title, content },
    headers: { Authorization: `Bearer ${token}` },
  })
  expect(res.status()).toBe(201)
  return (await res.json()).data.postId
}

export async function closePostViaApi(request: APIRequestContext, user: TestUser, postId: number) {
  const token = await loginViaApi(request, user)
  const res = await request.patch(`${API_URL}/study-posts/${postId}`, {
    data: { studyStatus: 'CLOSED' },
    headers: { Authorization: `Bearer ${token}` },
  })
  expect(res.status()).toBe(200)
}

export async function createCommentViaApi(
  request: APIRequestContext,
  user: TestUser,
  postId: number,
  content: string,
) {
  const token = await loginViaApi(request, user)
  const res = await request.post(`${API_URL}/study-posts/${postId}/comments`, {
    data: { content },
    headers: { Authorization: `Bearer ${token}` },
  })
  expect(res.status()).toBe(201)
}
