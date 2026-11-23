import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'
import { signupViaApi } from './helpers/auth'
import { createTestUser } from './helpers/user'
import type { TestUser } from './helpers/user'

async function fillSignupForm(page: Page, user: TestUser, generation = String(user.generation)) {
  await page.getByLabel('이메일', { exact: true }).fill(user.email)
  await page.getByLabel('비밀번호', { exact: true }).fill(user.password)
  await page.getByLabel('이름', { exact: true }).fill(user.name)
  await page.getByLabel('닉네임', { exact: true }).fill(user.nickname)
  await page.getByLabel('기수', { exact: true }).fill(generation)
  await page.getByLabel('파트', { exact: true }).selectOption(user.part)
}

async function fillLoginForm(page: Page, email: string, password: string) {
  await page.getByLabel('이메일', { exact: true }).fill(email)
  await page.getByLabel('비밀번호', { exact: true }).fill(password)
  await page.getByRole('button', { name: '로그인', exact: true }).click()
}

test.describe('회원 인증', () => {
  test('회원가입 → 로그인 → 마이페이지 확인 → 로그아웃', async ({ page }) => {
    const user = createTestUser({ generation: 13, part: 'DE' })
    const userMenu = page.getByRole('navigation', { name: '사용자 메뉴' })

    await test.step('회원가입', async () => {
      await page.goto('/signup')
      await fillSignupForm(page, user)
      await page.getByRole('button', { name: '회원가입', exact: true }).click()

      await expect(page).toHaveURL(/\/login$/)
      await expect(page.getByRole('status')).toHaveText('회원가입이 완료되었습니다. 로그인해주세요.')
    })

    await test.step('로그인하면 헤더에 닉네임이 보인다', async () => {
      await fillLoginForm(page, user.email, user.password)

      await expect(page).toHaveURL(/\/mypage$/)
      await expect(userMenu.getByText(user.nickname, { exact: true })).toBeVisible()
    })

    await test.step('마이페이지에서 기수, 파트를 확인한다', async () => {
      const main = page.getByRole('main')
      await expect(main.getByRole('heading', { name: '마이페이지' })).toBeVisible()
      await expect(main.getByText(user.email, { exact: true })).toBeVisible()
      await expect(main.getByText('13기', { exact: true })).toBeVisible()
      await expect(main.getByText('DE', { exact: true })).toBeVisible()
    })

    await test.step('로그아웃', async () => {
      await userMenu.getByRole('button', { name: '로그아웃' }).click()

      await expect(page).toHaveURL(/\/login$/)
      await expect(userMenu.getByRole('link', { name: '로그인', exact: true })).toBeVisible()
      await expect(userMenu.getByText(user.nickname, { exact: true })).toHaveCount(0)
    })
  })

  test('이미 가입된 이메일로는 가입할 수 없다', async ({ page, request }) => {
    const existing = createTestUser()
    await signupViaApi(request, existing)

    await page.goto('/signup')
    await fillSignupForm(page, createTestUser({ email: existing.email }))
    await page.getByRole('button', { name: '회원가입', exact: true }).click()

    await expect(page.getByRole('alert')).toHaveText('이미 가입된 이메일입니다.')
    await expect(page).toHaveURL(/\/signup$/)
  })

  test('기수를 0으로 입력하면 가입할 수 없다', async ({ page }) => {
    await page.goto('/signup')
    await fillSignupForm(page, createTestUser(), '0')
    await page.getByRole('button', { name: '회원가입', exact: true }).click()

    await expect(page.getByRole('alert')).toHaveText('입력값이 올바르지 않습니다.')
    await expect(page).toHaveURL(/\/signup$/)
  })

  test('비밀번호가 틀리면 로그인할 수 없다', async ({ page, request }) => {
    const user = createTestUser()
    await signupViaApi(request, user)
    const wrongPassword = createTestUser().password

    await page.goto('/login')
    await fillLoginForm(page, user.email, wrongPassword)

    await expect(page.getByRole('alert')).toHaveText('이메일 또는 비밀번호가 올바르지 않습니다.')
    await expect(page).toHaveURL(/\/login$/)
  })

  test('로그인 없이 마이페이지에 들어가면 로그인 화면으로 이동한다', async ({ page }) => {
    await page.goto('/mypage')

    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('heading', { name: '로그인' })).toBeVisible()
  })
})
