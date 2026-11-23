import { expect, test } from '@playwright/test'
import { callApi, createUser, errorCodeOf, newUser, signupByApi } from './helpers/api.ts'

test.describe('인증', () => {
  test('기수·파트로 가입하고 로그인한 뒤 헤더 닉네임과 마이페이지 정보를 확인하고 로그아웃한다', async ({ page }) => {
    const user = newUser({ generation: 14, part: 'BE' })

    // 회원가입
    await page.goto('/signup')
    await page.getByLabel('이메일').fill(user.email)
    await page.getByLabel('비밀번호').fill(user.password)
    await page.getByLabel('이름').fill(user.name)
    await page.getByLabel('닉네임').fill(user.nickname)
    await page.getByLabel('기수').fill(String(user.generation))
    await page.getByLabel('파트').selectOption('BE')
    await page.getByRole('button', { name: '회원가입' }).click()

    // 가입 후 로그인 화면으로 이동하고 안내 문구가 보인다
    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByText('가입이 완료되었어요. 로그인해 주세요.')).toBeVisible()

    // 로그인
    await page.getByLabel('이메일').fill(user.email)
    await page.getByLabel('비밀번호').fill(user.password)
    await page.getByRole('button', { name: '로그인' }).click()
    await expect(page).toHaveURL(/\/studies$/)

    // 헤더에 닉네임이 보인다
    const header = page.getByRole('banner')
    await expect(header.getByText(user.nickname)).toBeVisible()

    // 마이페이지에서 가입할 때 입력한 기수와 파트를 확인한다
    await header.getByRole('link', { name: '마이페이지' }).click()
    await expect(page).toHaveURL(/\/mypage$/)
    await expect(page.getByText(user.email)).toBeVisible()
    await expect(page.getByText('14기', { exact: true })).toBeVisible()
    await expect(page.getByText('백엔드 (BE)', { exact: true })).toBeVisible()

    // 로그아웃하면 로그인 화면으로 가고, 헤더가 비로그인 상태로 바뀐다
    await header.getByRole('button', { name: '로그아웃' }).click()
    await expect(page).toHaveURL(/\/login$/)
    await expect(header.getByRole('link', { name: '회원가입' })).toBeVisible()
    await expect(header.getByText(user.nickname)).toHaveCount(0)

    // 로그아웃한 뒤에는 마이페이지에 들어갈 수 없다
    await page.goto('/mypage')
    await expect(page).toHaveURL(/\/login$/)
  })

  test('이미 가입된 이메일로 가입하면 오류 메시지를 보여준다', async ({ page, request }) => {
    const existing = newUser()
    await signupByApi(request, existing)

    await page.goto('/signup')
    await page.getByLabel('이메일').fill(existing.email)
    await page.getByLabel('비밀번호').fill(newUser().password)
    await page.getByLabel('이름').fill('다른 사람')
    await page.getByLabel('닉네임').fill(newUser().nickname)
    await page.getByLabel('기수').fill('14')
    await page.getByLabel('파트').selectOption('FE')
    await page.getByRole('button', { name: '회원가입' }).click()

    await expect(page.getByText('이미 가입된 이메일입니다.')).toBeVisible()
    await expect(page).toHaveURL(/\/signup$/)
  })

  test('기수에 0을 입력하면 화면과 서버가 모두 가입을 거부한다', async ({ page, request }) => {
    const user = newUser()

    await page.goto('/signup')
    await page.getByLabel('이메일').fill(user.email)
    await page.getByLabel('비밀번호').fill(user.password)
    await page.getByLabel('이름').fill(user.name)
    await page.getByLabel('닉네임').fill(user.nickname)
    await page.getByLabel('기수').fill('0')
    await page.getByLabel('파트').selectOption('BE')
    await page.getByRole('button', { name: '회원가입' }).click()

    await expect(page.getByText('기수는 1 이상의 정수로 입력해 주세요.')).toBeVisible()
    await expect(page).toHaveURL(/\/signup$/)

    // 화면 검사를 거치지 않고 API를 직접 호출해도 서버가 400으로 막는다
    const response = await callApi(request, 'POST', '/api/auth/signup', { data: { ...user, generation: 0 } })
    expect(response.status()).toBe(400)
    expect(await errorCodeOf(response)).toBe('COMMON-003')
  })

  test('비밀번호가 틀리면 로그인할 수 없다', async ({ page, request }) => {
    const user = newUser()
    await signupByApi(request, user)

    await page.goto('/login')
    await page.getByLabel('이메일').fill(user.email)
    await page.getByLabel('비밀번호').fill(newUser().password) // 가입할 때와 다른 무작위 비밀번호
    await page.getByRole('button', { name: '로그인' }).click()

    await expect(page.getByText('이메일 또는 비밀번호가 올바르지 않습니다.')).toBeVisible()
    await expect(page).toHaveURL(/\/login$/)
  })

  test('로그인 없이 마이페이지에 들어가면 로그인 화면으로 보낸다', async ({ page }) => {
    await page.goto('/mypage')
    await expect(page).toHaveURL(/\/login$/)
  })

  test('로그아웃한 토큰은 더 이상 사용할 수 없다', async ({ request }) => {
    const { token } = await createUser(request)

    const before = await callApi(request, 'GET', '/api/users/me', { token })
    expect(before.status()).toBe(200)

    const logout = await callApi(request, 'POST', '/api/auth/logout', { token })
    expect(logout.ok()).toBeTruthy()

    const after = await callApi(request, 'GET', '/api/users/me', { token })
    expect(after.status()).toBe(401)
    expect(await errorCodeOf(after)).toBe('COMMON-004')
  })
})
