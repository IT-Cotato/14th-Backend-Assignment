import { expect, test } from '@playwright/test'
import { API_URL, expectError, newUser, signupViaApi } from './helpers.ts'

test.describe('인증', () => {
  test('기수와 파트를 입력해 회원가입하고, 로그인하면 헤더와 마이페이지에 내 정보가 보이고, 로그아웃할 수 있다', async ({
    page,
  }) => {
    const user = newUser('FE', 14)

    await page.goto('/signup')
    await page.getByLabel('이메일').fill(user.email)
    await page.getByLabel('비밀번호').fill(user.password)
    await page.getByLabel('이름').fill(user.name)
    await page.getByLabel('닉네임').fill(user.nickname)
    await page.getByLabel('기수').fill(String(user.generation))
    await page.getByLabel('파트').selectOption(user.part)
    await page.getByRole('button', { name: '회원가입' }).click()

    await expect(page).toHaveURL(/\/login$/)
    await page.getByLabel('이메일').fill(user.email)
    await page.getByLabel('비밀번호').fill(user.password)
    await page.getByRole('button', { name: '로그인', exact: true }).click()

    await expect(page).toHaveURL(/\/studies$/)
    const userMenu = page.getByRole('navigation', { name: '사용자 메뉴' })
    await expect(userMenu.getByText(`${user.nickname}님`)).toBeVisible()

    await userMenu.getByRole('link', { name: '마이페이지' }).click()
    await expect(page).toHaveURL(/\/mypage$/)
    await expect(page.getByRole('heading', { name: '마이페이지' })).toBeVisible()
    await expect(page.getByText(user.email)).toBeVisible()
    await expect(page.getByText('14기')).toBeVisible()
    await expect(page.getByText('FE (프론트엔드)')).toBeVisible()

    await userMenu.getByRole('button', { name: '로그아웃' }).click()
    await expect(page).toHaveURL(/\/login$/)
    await expect(userMenu.getByRole('link', { name: '회원가입' })).toBeVisible()
    await expect(userMenu.getByText(`${user.nickname}님`)).toHaveCount(0)
  })

  test('이미 가입된 이메일로 가입하면 서버 에러 메시지를 보여준다', async ({ page, request }) => {
    const user = newUser()
    await signupViaApi(request, user)

    await page.goto('/signup')
    await page.getByLabel('이메일').fill(user.email)
    await page.getByLabel('비밀번호').fill(user.password)
    await page.getByLabel('이름').fill(user.name)
    await page.getByLabel('닉네임').fill(user.nickname)
    await page.getByLabel('기수').fill('14')
    await page.getByLabel('파트').selectOption('BE')
    await page.getByRole('button', { name: '회원가입' }).click()

    await expect(page.getByRole('alert')).toHaveText('이미 가입된 이메일이에요.')
    await expect(page).toHaveURL(/\/signup$/)
  })

  test('기수에 0을 넣으면 가입되지 않고 검증 메시지를 보여주며, API도 400을 내려준다', async ({ page, request }) => {
    const user = newUser()

    await page.goto('/signup')
    await page.getByLabel('이메일').fill(user.email)
    await page.getByLabel('비밀번호').fill(user.password)
    await page.getByLabel('이름').fill(user.name)
    await page.getByLabel('닉네임').fill(user.nickname)
    await page.getByLabel('기수').fill('0')
    await page.getByLabel('파트').selectOption('BE')
    await page.getByRole('button', { name: '회원가입' }).click()

    await expect(page.getByText('기수는 1 이상의 정수여야 해요.')).toBeVisible()
    await expect(page.getByLabel('기수')).toHaveAttribute('aria-invalid', 'true')
    await expect(page).toHaveURL(/\/signup$/)

    // 화면 검증을 거치지 않고 보내도 서버가 막는다
    const response = await request.post(`${API_URL}/api/auth/signup`, { data: { ...user, generation: 0 } })
    await expectError(response, 400, 'COMMON-003')
  })

  test('비밀번호가 틀리면 로그인되지 않고 서버 에러 메시지를 보여준다', async ({ page, request }) => {
    const user = newUser()
    await signupViaApi(request, user)

    await page.goto('/login')
    await page.getByLabel('이메일').fill(user.email)
    await page.getByLabel('비밀번호').fill(`${user.password}-wrong`)
    await page.getByRole('button', { name: '로그인', exact: true }).click()

    await expect(page.getByRole('alert')).toHaveText('이메일 또는 비밀번호가 틀려요.')
    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('navigation', { name: '사용자 메뉴' }).getByRole('link', { name: '로그인' })).toBeVisible()
  })

  test('로그인하지 않고 마이페이지에 들어가면 로그인 화면으로 이동하고, API는 401을 내려준다', async ({
    page,
    request,
  }) => {
    await page.goto('/mypage')

    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('heading', { name: '로그인' })).toBeVisible()

    const response = await request.get(`${API_URL}/api/users/me`)
    await expectError(response, 401, 'COMMON-004')
  })
})
