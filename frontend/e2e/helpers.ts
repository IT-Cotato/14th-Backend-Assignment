import { expect, type APIRequestContext, type Page } from '@playwright/test'
import { randomUUID } from 'node:crypto'

/** 테스트 계정 비밀번호: 환경 변수가 없으면 실행할 때마다 새로 만들어요 (코드에 적지 않아요) */
export const PASSWORD = process.env.E2E_PASSWORD ?? randomUUID()

export type Part = 'PM' | 'DE' | 'BE' | 'FE'

export interface TestUser {
  email: string
  password: string
  name: string
  nickname: string
  generation: number
  part: Part
}

export const uid = () => randomUUID().slice(0, 8)

/** 매번 새 사용자. 이메일은 e2e- 로 시작해서 직접 만든 데이터와 구분해요 */
export function newUser(overrides: Partial<TestUser> = {}): TestUser {
  const id = uid()
  return {
    email: `e2e-${id}@test.com`,
    password: PASSWORD,
    name: '테스트',
    nickname: `e2e${id.slice(0, 6)}`,
    generation: 14,
    part: 'BE',
    ...overrides,
  }
}

export async function signupByApi(request: APIRequestContext, user: TestUser) {
  const res = await request.post('/api/auth/signup', { data: user })
  expect(res.status()).toBe(201)
}

export async function loginByApi(request: APIRequestContext, user: TestUser): Promise<string> {
  const res = await request.post('/api/auth/login', { data: { email: user.email, password: user.password } })
  expect(res.status()).toBe(200)
  return (await res.json()).data.accessToken
}

/** 가입 + 토큰 발급 */
export async function createUser(request: APIRequestContext, overrides: Partial<TestUser> = {}) {
  const user = newUser(overrides)
  await signupByApi(request, user)
  return { user, token: await loginByApi(request, user) }
}

export const auth = (token: string) => ({ Authorization: `Bearer ${token}` })

export async function createPostByApi(request: APIRequestContext, token: string, title = `e2e 스터디 ${uid()}`) {
  const res = await request.post('/api/study-posts', { headers: auth(token), data: { title, content: `${title} 내용` } })
  expect(res.status()).toBe(201)
  return { postId: (await res.json()).data.postId as number, title }
}

export async function loginByUi(page: Page, user: TestUser) {
  await page.goto('/login')
  await page.getByLabel('이메일', { exact: true }).fill(user.email)
  await page.getByLabel('비밀번호', { exact: true }).fill(user.password)
  await page.getByRole('button', { name: '로그인' }).click()
  await expect(page).toHaveURL(/\/studies$/)
  await expect(page.getByText(`${user.nickname}님`)).toBeVisible()
}
