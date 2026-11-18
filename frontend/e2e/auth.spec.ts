import { expect, test } from '@playwright/test'
import { createUserAndLogin, loginViaUi, newUser, signupViaApi } from './helpers.ts'

test.describe('인증', () => {
  test('회원가입한 뒤 로그인하면 마이페이지에서 내 정보를 볼 수 있다', async ({ page }) => {
    const user = newUser()

    await page.goto('/signup')
    await page.getByLabel('이메일').fill(user.email)
    await page.getByLabel('비밀번호').fill(user.password)
    await page.getByLabel('이름').fill(user.name)
    await page.getByLabel('닉네임').fill(user.nickname)
    await page.getByLabel('기수').fill(String(user.generation))
    await page.getByLabel('파트').selectOption('BE')
    await page.getByRole('button', { name: '가입하기' }).click()
    await expect(page).toHaveURL('/login')

    await loginViaUi(page, user)
    await page.getByRole('link', { name: '마이페이지' }).click()
    await expect(page.getByRole('heading', { name: '마이페이지' })).toBeVisible()
    await expect(page.getByText(user.email)).toBeVisible()
    await expect(page.getByText(`${user.generation}기`)).toBeVisible()
    await expect(page.getByText('백엔드 (BE)')).toBeVisible()
  })

  test('이미 가입된 이메일로 가입하면 오류 메시지가 보인다', async ({ page, request }) => {
    const user = newUser()
    await signupViaApi(request, user)

    await page.goto('/signup')
    await page.getByLabel('이메일').fill(user.email)
    await page.getByLabel('비밀번호').fill(user.password)
    await page.getByLabel('이름').fill(user.name)
    await page.getByLabel('닉네임').fill(newUser().nickname)
    await page.getByLabel('기수').fill(String(user.generation))
    await page.getByLabel('파트').selectOption('BE')
    await page.getByRole('button', { name: '가입하기' }).click()

    await expect(page.getByRole('alert')).toHaveText('이미 가입된 이메일이에요.')
    await expect(page).toHaveURL('/signup')
  })

  test('비밀번호가 8자 미만이면 가입 전에 안내가 보인다', async ({ page }) => {
    const user = newUser()

    await page.goto('/signup')
    await page.getByLabel('이메일').fill(user.email)
    await page.getByLabel('비밀번호').fill('short')
    await page.getByRole('button', { name: '가입하기' }).click()

    await expect(page.getByText('비밀번호는 8자 이상이에요.')).toBeVisible()
    await expect(page).toHaveURL('/signup')
  })

  test('기수에 0을 입력하면 가입 전에 안내가 보인다', async ({ page }) => {
    const user = newUser()

    await page.goto('/signup')
    await page.getByLabel('이메일').fill(user.email)
    await page.getByLabel('비밀번호').fill(user.password)
    await page.getByLabel('이름').fill(user.name)
    await page.getByLabel('닉네임').fill(user.nickname)
    await page.getByLabel('기수').fill('0')
    await page.getByLabel('파트').selectOption('BE')
    await page.getByRole('button', { name: '가입하기' }).click()

    await expect(page.getByText('기수는 1 이상의 정수예요.')).toBeVisible()
    await expect(page).toHaveURL('/signup')
  })

  test('비밀번호가 틀리면 로그인되지 않고 오류 메시지가 보인다', async ({ page, request }) => {
    const user = newUser()
    await signupViaApi(request, user)

    await page.goto('/login')
    await page.getByLabel('이메일').fill(user.email)
    await page.getByLabel('비밀번호').fill(`${user.password}-wrong`)
    await page.getByRole('button', { name: '로그인' }).click()

    await expect(page.getByRole('alert')).toHaveText('이메일 또는 비밀번호가 올바르지 않아요.')
    await expect(page).toHaveURL('/login')
  })

  test('로그아웃하면 로그인 링크가 다시 보인다', async ({ page, request }) => {
    const user = await createUserAndLogin(page, request)

    await page.getByRole('button', { name: '로그아웃' }).click()

    await expect(page).toHaveURL('/login')
    await expect(page.getByRole('link', { name: '회원가입' }).first()).toBeVisible()
    await expect(page.getByText(user.nickname)).toHaveCount(0)
  })

  test('로그인하지 않고 마이페이지에 들어가면 로그인 화면으로 간다', async ({ page }) => {
    await page.goto('/mypage')

    await expect(page).toHaveURL('/login')
  })
})
