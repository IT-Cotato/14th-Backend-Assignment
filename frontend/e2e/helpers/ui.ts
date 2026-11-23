import { expect, type Page } from '@playwright/test'
import type { TestUser } from './api.ts'

// 화면에서 로그인하고, 헤더에 닉네임이 보일 때까지 기다린다 (내 정보 조회까지 끝난 상태)
export async function loginByUi(page: Page, user: Pick<TestUser, 'email' | 'password' | 'nickname'>): Promise<void> {
  await page.goto('/login')
  await page.getByLabel('이메일').fill(user.email)
  await page.getByLabel('비밀번호').fill(user.password)
  await page.getByRole('button', { name: '로그인' }).click()
  await expect(page).toHaveURL(/\/studies$/)
  await expect(page.getByRole('banner').getByText(user.nickname)).toBeVisible()
}
