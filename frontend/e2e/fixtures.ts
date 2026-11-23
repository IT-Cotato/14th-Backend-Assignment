import { randomUUID } from 'node:crypto'
import { expect } from '@playwright/test'
import type { APIRequestContext, Page } from '@playwright/test'

const origin = process.env.VITE_API_URL ?? 'http://localhost:8080'
export const apiUrl = (path: string) => origin + '/api' + path
export const bearer = (token: string) => ({ Authorization: `Bearer ${token}` })
export const account = () => {
  const id = randomUUID()
  return { email: `e2e-${id}@example.com`, password: randomUUID(), name: '테스트 회원',
    nickname: `테스트${id.slice(0, 6)}`, generation: 14, part: 'BE' }
}

export async function register(request: APIRequestContext, data = account()) {
  const response = await request.post(apiUrl('/auth/signup'), { data })
  expect(response.status()).toBe(201)
  return data
}

export async function tokenFor(request: APIRequestContext, data: ReturnType<typeof account>) {
  const response = await request.post(apiUrl('/auth/login'), { data: { email: data.email, password: data.password } })
  expect(response.status()).toBe(200)
  return (await response.json()).data.accessToken as string
}

export async function login(page: Page, data: ReturnType<typeof account>) {
  await page.goto('/login')
  await page.getByLabel('이메일').fill(data.email)
  await page.getByLabel('비밀번호').fill(data.password)
  await page.getByRole('button', { name: '로그인', exact: true }).click()
  await expect(page).toHaveURL(/\/studies$/)
}

export async function fillSignup(page: Page, data: ReturnType<typeof account>) {
  await page.goto('/signup')
  await page.getByLabel('이메일').fill(data.email)
  await page.getByLabel('비밀번호').fill(data.password)
  await page.getByLabel('이름', { exact: true }).fill(data.name)
  await page.getByLabel('닉네임').fill(data.nickname)
  await page.getByLabel('기수').fill(String(data.generation))
  await page.getByRole('combobox', { name: '파트', exact: true }).selectOption(data.part)
  await page.getByRole('button', { name: '회원가입' }).click()
}

export async function makeStudy(request: APIRequestContext, token: string) {
  const title = `스터디 ${randomUUID()}`
  const response = await request.post(apiUrl('/study-posts'), {
    headers: bearer(token), data: { title, content: '매주 코드 리뷰를 진행해요.' },
  })
  expect(response.status()).toBe(201)
  return { id: (await response.json()).data.postId as number, title }
}
