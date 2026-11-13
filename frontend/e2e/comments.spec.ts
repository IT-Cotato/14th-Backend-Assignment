import { test, expect } from '@playwright/test'
import { apiUrl, bearer, login, makeStudy, register, tokenFor } from './fixtures'

test('신청 댓글 작성 → 수정 → 삭제', async ({ page, request }) => {
  const owner = await register(request)
  const post = await makeStudy(request, await tokenFor(request, owner))
  await login(page, await register(request))
  await page.goto(`/studies/${post.id}`)
  await page.getByLabel('신청 댓글').fill('스터디에 신청합니다.')
  await page.getByRole('button', { name: '댓글 등록' }).click()
  await expect(page.getByText('스터디에 신청합니다.', { exact: true })).toBeVisible()
  const count = await request.get(apiUrl(`/study-posts?keyword=${encodeURIComponent(post.title)}`))
  expect((await count.json()).data.content[0].commentCount).toBe(1)
  await page.getByRole('button', { name: '댓글 수정' }).click()
  await page.getByLabel('댓글 내용').fill('목요일에 참여할 수 있어요.')
  await page.getByRole('button', { name: '댓글 저장' }).click()
  await expect(page.getByText('목요일에 참여할 수 있어요.')).toBeVisible()
  await page.getByRole('button', { name: '댓글 삭제' }).click()
  await expect(page.getByText('아직 댓글이 없어요.')).toBeVisible()
})

test('마감된 글의 신청과 다른 사용자의 댓글 수정·삭제를 차단한다', async ({ page, request }) => {
  const author = await register(request)
  const headers = bearer(await tokenFor(request, author))
  const post = await makeStudy(request, headers.Authorization.substring(7))
  const postUrl = apiUrl(`/study-posts/${post.id}`)
  const input = { content: '참여하고 싶어요.' }
  const added = await request.post(postUrl + '/comments', { headers, data: input })
  expect(added.status()).toBe(201)
  const { commentId } = (await added.json()).data
  expect((await request.patch(postUrl, { headers, data: { studyStatus: 'CLOSED' } })).status()).toBe(200)
  const member = await register(request)
  const memberHeaders = bearer(await tokenFor(request, member))
  await login(page, member)
  await page.goto(`/studies/${post.id}`)
  await expect(page.getByText('모집이 마감된 글이에요.')).toBeVisible()
  await expect(page.getByRole('button', { name: '댓글 등록' })).toHaveCount(0)
  await expect(page.getByText(input.content)).toBeVisible()
  await expect(page.getByRole('button', { name: '댓글 수정' })).toHaveCount(0)
  const closed = await request.post(postUrl + '/comments', { headers: memberHeaders, data: input })
  expect(closed.status()).toBe(409)
  expect((await closed.json()).code).toBe('STUDY-003')
  const commentUrl = apiUrl(`/comments/${commentId}`)
  for (const response of [await request.patch(commentUrl, { headers: memberHeaders, data: input }),
    await request.delete(commentUrl, { headers: memberHeaders })]) {
    expect(response.status()).toBe(403)
    expect((await response.json()).code).toBe('COMMENT-002')
  }
  expect((await request.delete(postUrl, { headers })).status()).toBe(204)
  expect((await request.delete(commentUrl, { headers })).status()).toBe(404)
})
