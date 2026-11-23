import type { Page } from '@playwright/test'
import { expectError } from './support/api.ts'
import { newUser, type TestUser } from './support/data.ts'
import { expect, test } from './support/fixtures.ts'

async function fillSignupForm(page: Page, user: TestUser, generation = String(user.generation)) {
  await page.getByLabel('이메일').fill(user.email)
  await page.getByLabel('비밀번호').fill(user.password)
  await page.getByLabel('이름', { exact: true }).fill(user.name)
  await page.getByLabel('닉네임').fill(user.nickname)
  await page.getByLabel('기수').fill(generation)
  await page.getByLabel('파트').selectOption(user.part)
}

async function fillLoginForm(page: Page, email: string, password: string) {
  await page.getByLabel('이메일').fill(email)
  await page.getByLabel('비밀번호').fill(password)
  await page.getByRole('button', { name: '로그인', exact: true }).click()
}

test.describe('인증', () => {
  test('기수·파트를 넣어 가입하고 로그인하면 헤더에 닉네임이, 마이페이지에 기수·파트가 보이고 로그아웃할 수 있어요', async ({
    page,
  }) => {
    const user = newUser({ generation: 14, part: 'BE' })
    const userMenu = page.getByRole('navigation', { name: '사용자 메뉴' })

    await test.step('회원가입', async () => {
      await page.goto('/signup')
      await expect(page.getByRole('heading', { name: '회원가입' })).toBeVisible()
      await fillSignupForm(page, user)
      await page.getByRole('button', { name: '회원가입', exact: true }).click()

      await expect(page).toHaveURL(/\/login$/)
      await expect(page.getByText('회원가입이 완료됐어요. 로그인해 주세요.')).toBeVisible()
    })

    await test.step('로그인하면 헤더에 닉네임이 보여요', async () => {
      await fillLoginForm(page, user.email, user.password)

      await expect(page).toHaveURL(/\/studies$/)
      await expect(userMenu.getByText(user.nickname)).toBeVisible()
      await expect(userMenu.getByRole('button', { name: '로그아웃' })).toBeVisible()
    })

    await test.step('마이페이지에서 기수·파트를 확인해요', async () => {
      await userMenu.getByRole('link', { name: '마이페이지' }).click()

      await expect(page).toHaveURL(/\/mypage$/)
      const main = page.getByRole('main')
      await expect(main.getByRole('heading', { name: '마이페이지' })).toBeVisible()
      await expect(main.getByText(user.email)).toBeVisible()
      await expect(main.getByText(user.nickname)).toBeVisible()
      await expect(main.getByText('14기')).toBeVisible()
      await expect(main.getByText('BE (백엔드)')).toBeVisible()
    })

    await test.step('로그아웃하면 로그인 화면으로 가고 헤더가 바뀌어요', async () => {
      await userMenu.getByRole('button', { name: '로그아웃' }).click()

      await expect(page).toHaveURL(/\/login$/)
      await expect(userMenu.getByRole('link', { name: '로그인', exact: true })).toBeVisible()
      await expect(userMenu.getByText(user.nickname)).toHaveCount(0)
      expect(await page.evaluate('window.localStorage.getItem("accessToken")')).toBeNull()
    })
  })

  test('이미 가입된 이메일이면 서버 메시지를 보여주고 가입 화면에 남아요', async ({ page, api }) => {
    const existing = newUser()
    expect((await api.signup(existing)).status()).toBe(201)

    await page.goto('/signup')
    await fillSignupForm(page, { ...newUser(), email: existing.email })
    await page.getByRole('button', { name: '회원가입', exact: true }).click()

    await expect(page.getByRole('alert')).toHaveText('이미 가입된 이메일이에요.')
    await expect(page).toHaveURL(/\/signup$/)
  })

  test('기수를 0으로 넣으면 가입되지 않아요', async ({ page, api }) => {
    const user = newUser()

    await page.goto('/signup')
    await fillSignupForm(page, user, '0')
    await page.getByRole('button', { name: '회원가입', exact: true }).click()

    await expect(page.getByText('기수는 1 이상의 정수로 입력해 주세요.')).toBeVisible()
    await expect(page).toHaveURL(/\/signup$/)

    // 화면 검사를 건너뛰고 API로 보내도 서버가 막아요
    await expectError(await api.signup({ ...user, generation: 0 }), 400, 'COMMON-003')
  })

  test('비밀번호가 틀리면 서버 메시지를 보여주고 로그인되지 않아요', async ({ page, createUser }) => {
    const user = await createUser()

    await page.goto('/login')
    await fillLoginForm(page, user.email, `${user.password}-wrong`)

    await expect(page.getByRole('alert')).toHaveText('이메일 또는 비밀번호가 틀려요.')
    await expect(page).toHaveURL(/\/login$/)
    expect(await page.evaluate('window.localStorage.getItem("accessToken")')).toBeNull()
  })

  test('로그인하지 않고 마이페이지에 들어가면 로그인 화면으로 보내요', async ({ page, api }) => {
    await page.goto('/mypage')

    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('heading', { name: '로그인' })).toBeVisible()

    // 토큰 없이 내 정보 API를 부르면 401이에요
    await expectError(await api.getMe(), 401, 'COMMON-004')
  })
})
