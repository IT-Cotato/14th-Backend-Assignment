import { randomUUID } from 'node:crypto'
import { test, expect } from '@playwright/test'
import { account, apiUrl, bearer, login, makeStudy, register, tokenFor } from './fixtures'

test('모집글 작성 → 검색과 필터 → 수정 → 마감 → 삭제', async ({ page, request }) => {
  const member = await register(request)
  const other = await register(request, { ...account(), part: 'FE' })
  const title = `알고리즘 ${randomUUID()}`
  const otherToken = await tokenFor(request, other)
  expect((await request.post(apiUrl('/study-posts'), {
    headers: bearer(otherToken), data: { title, content: '같은 제목, 다른 파트' },
  })).status()).toBe(201)
  await login(page, member)
  await page.getByRole('link', { name: '모집글 작성' }).click()
  await page.getByLabel('제목').fill(title)
  await page.getByLabel('내용').fill('주 2회 알고리즘을 공부해요.')
  await page.getByRole('button', { name: '저장' }).click()
  await expect(page.getByRole('heading', { name: title })).toBeVisible()
  await page.getByRole('link', { name: '목록으로' }).click()
  await page.getByLabel('키워드').fill(title)
  await page.getByRole('combobox', { name: '모집 상태' }).selectOption('CLOSED')
  await page.getByRole('combobox', { name: '작성자 파트' }).selectOption('BE')
  await page.getByRole('button', { name: '검색' }).click()
  await expect(page.getByText('조건에 맞는 모집글이 없어요.')).toBeVisible()
  await page.getByRole('combobox', { name: '모집 상태' }).selectOption('RECRUITING')
  await page.getByRole('button', { name: '검색' }).click()
  await expect(page.getByRole('link', { name: title })).toHaveCount(1)
  await page.getByRole('link', { name: title }).click()
  await expect(page.getByText('주 2회 알고리즘을 공부해요.')).toBeVisible()
  await page.getByRole('link', { name: '글 수정' }).click()
  await page.getByLabel('제목').fill(title + ' 수정')
  await page.getByRole('button', { name: '저장' }).click()
  await expect(page.getByRole('heading', { name: title + ' 수정' })).toBeVisible()
  await page.getByRole('button', { name: '모집 마감' }).click()
  await expect(page.getByText('모집이 마감된 글이에요.')).toBeVisible()
  const url = page.url()
  await page.getByRole('button', { name: '글 삭제' }).click()
  await expect(page).toHaveURL(/\/studies$/)
  await page.goto(url)
  await expect(page.getByRole('alert')).toHaveText('존재하지 않는 스터디 모집글이에요.')
})

test('비로그인 작성과 다른 작성자의 글 수정·삭제를 차단한다', async ({ page, request }) => {
  const owner = await register(request)
  const post = await makeStudy(request, await tokenFor(request, owner))
  await page.goto('/studies/new')
  await expect(page).toHaveURL(/\/login$/)
  const input = { title: post.title, content: '수정 시도' }
  expect((await request.post(apiUrl('/study-posts'), { data: input })).status()).toBe(401)
  const visitor = await register(request)
  await login(page, visitor)
  await page.goto(`/studies/${post.id}`)
  await expect(page.getByRole('heading', { name: post.title })).toBeVisible()
  await expect(page.getByRole('link', { name: '글 수정' })).toHaveCount(0)
  await expect(page.getByRole('button', { name: '글 삭제' })).toHaveCount(0)
  const headers = bearer(await tokenFor(request, visitor))
  const url = apiUrl(`/study-posts/${post.id}`)
  for (const response of [await request.patch(url, { headers, data: input }), await request.delete(url, { headers })]) {
    expect(response.status()).toBe(403)
    expect((await response.json()).code).toBe('STUDY-002')
  }
})
