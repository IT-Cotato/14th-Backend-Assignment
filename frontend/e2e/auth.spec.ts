import { randomUUID } from 'node:crypto'
import { expect, test, type Page } from '@playwright/test'
import {
  API_URL,
  authHeader,
  loginViaUi,
  newUser,
  signupViaApi,
  type TestUser,
} from './helpers.ts'

async function fillSignupForm(page: Page, user: TestUser, generation = String(user.generation)) {
  await page.getByLabel('이메일', { exact: true }).fill(user.email)
  await page.getByLabel('비밀번호', { exact: true }).fill(user.password)
  await page.getByLabel('이름', { exact: true }).fill(user.name)
  await page.getByLabel('닉네임', { exact: true }).fill(user.nickname)
  await page.getByLabel('기수', { exact: true }).fill(generation)
  await page.getByLabel('파트', { exact: true }).selectOption(user.part)
}

test.describe('인증', () => {
  test('기수와 파트를 입력해 회원가입하고 로그인하면 헤더와 마이페이지에서 내 정보를 확인하고 로그아웃할 수 있다', async ({
    page,
  }) => {
    const user = newUser({ generation: 14, part: 'BE' })

    await page.goto('/signup')
    await fillSignupForm(page, user)
    await page.getByRole('button', { name: '회원가입', exact: true }).click()
    await expect(page).toHaveURL(/\/login$/)

    await page.getByLabel('이메일', { exact: true }).fill(user.email)
    await page.getByLabel('비밀번호', { exact: true }).fill(user.password)
    await page.getByRole('button', { name: '로그인', exact: true }).click()
    await expect(page).toHaveURL(/\/studies$/)

    await expect(page.getByText(`${user.nickname}님`)).toBeVisible()
    await expect(page.getByRole('button', { name: '로그아웃' })).toBeVisible()
    await expect(page.getByRole('link', { name: '로그인', exact: true })).toHaveCount(0)

    await page.getByRole('link', { name: '마이페이지' }).click()
    await expect(page).toHaveURL(/\/mypage$/)
    await expect(page.getByRole('heading', { name: '마이페이지' })).toBeVisible()
    await expect(page.getByText(user.email)).toBeVisible()
    await expect(page.getByText(user.nickname, { exact: true })).toBeVisible()
    await expect(page.getByText('14기', { exact: true })).toBeVisible()
    await expect(page.getByText('BE (백엔드)', { exact: true })).toBeVisible()

    await page.getByRole('button', { name: '로그아웃' }).click()
    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('link', { name: '로그인', exact: true })).toBeVisible()
    await expect(page.getByRole('link', { name: '회원가입', exact: true })).toBeVisible()
  })

  test('이미 가입된 이메일로 회원가입하면 에러 메시지를 보여준다', async ({ page, request }) => {
    const existing = newUser()
    await signupViaApi(request, existing)

    await page.goto('/signup')
    await fillSignupForm(page, newUser({ email: existing.email }))
    await page.getByRole('button', { name: '회원가입', exact: true }).click()

    await expect(page.getByRole('alert')).toHaveText('이미 가입된 이메일입니다.')
    await expect(page).toHaveURL(/\/signup$/)
  })

  test('기수에 0을 입력하면 서버의 검증 메시지를 보여준다', async ({ page }) => {
    await page.goto('/signup')
    await fillSignupForm(page, newUser(), '0')
    await page.getByRole('button', { name: '회원가입', exact: true }).click()

    await expect(page.getByRole('alert')).toHaveText('기수는 1 이상의 정수여야 합니다.')
    await expect(page).toHaveURL(/\/signup$/)
  })

  test('틀린 비밀번호로 로그인하면 에러 메시지를 보여준다', async ({ page, request }) => {
    const user = newUser()
    await signupViaApi(request, user)

    await page.goto('/login')
    await page.getByLabel('이메일', { exact: true }).fill(user.email)
    await page.getByLabel('비밀번호', { exact: true }).fill(`wrong-${randomUUID()}`)
    await page.getByRole('button', { name: '로그인', exact: true }).click()

    await expect(page.getByRole('alert')).toHaveText('이메일 또는 비밀번호가 올바르지 않습니다.')
    await expect(page).toHaveURL(/\/login$/)
  })

  test('로그인 없이 마이페이지에 들어가면 로그인 화면으로 보낸다', async ({ page }) => {
    await page.goto('/mypage')

    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('button', { name: '로그인', exact: true })).toBeVisible()
  })

  test('로그아웃한 토큰으로 다시 요청하면 서버가 401로 막는다', async ({ page, request }) => {
    const user = newUser()
    await signupViaApi(request, user)
    await loginViaUi(page, user)

    const token = await page.evaluate(() => localStorage.getItem('accessToken'))
    expect(token).toBeTruthy()

    await page.getByRole('button', { name: '로그아웃' }).click()
    await expect(page).toHaveURL(/\/login$/)
    expect(await page.evaluate(() => localStorage.getItem('accessToken'))).toBeNull()

    const response = await request.get(`${API_URL}/api/users/me`, {
      headers: authHeader(token ?? ''),
    })
    expect(response.status()).toBe(401)
    expect((await response.json()).code).toBe('COMMON-004')
  })
})
