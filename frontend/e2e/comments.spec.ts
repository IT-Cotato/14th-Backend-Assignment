import { expect, test } from '@playwright/test'
import { createComment, createPost, expectApiError, loginUi, register } from './fixtures'

const detail = (postId: number) => '/study-posts/' + postId

test('댓글 작성·수정·삭제가 공개 목록과 댓글 수에 반영된다', async ({ page, request }) => {
  const owner = await register(request)
  const writer = await register(request, 'FE')
  const id = await createPost(request, owner.headers)
  await loginUi(page, writer.account)
  await page.goto(detail(id))
  const list = page.getByRole('region', { name: /^댓글/ })
  await expect(list.getByText('아직 댓글이 없습니다.')).toBeVisible()
  await list.getByLabel('댓글 내용').fill('참여 신청')
  await list.getByRole('button', { name: '댓글 등록' }).click()
  await expect(list.getByRole('heading', { name: '댓글 1개' })).toBeVisible()
  await expect(list.getByText(writer.account.nickname + ' · 14기 · FE')).toBeVisible()
  await list.getByRole('button', { name: '댓글 수정' }).click()
  await expect(list.getByLabel('댓글 수정 내용')).toHaveValue('참여 신청')
  await list.getByLabel('댓글 수정 내용').fill('참여 신청을 수정합니다')
  await list.getByRole('button', { name: '댓글 저장' }).click()
  await expect(list.getByText('참여 신청을 수정합니다', { exact: true })).toBeVisible()
  await list.getByRole('button', { name: '댓글 삭제', exact: true }).click()
  const confirmation = list.getByRole('group', { name: '댓글 삭제 확인' })
  await confirmation.getByRole('button', { name: '취소' }).click()
  await expect(list.getByText('참여 신청을 수정합니다', { exact: true })).toBeVisible()
  await list.getByRole('button', { name: '댓글 삭제', exact: true }).click()
  const response = page.waitForResponse((res) => res.url().includes('/api/comments/') && res.request().method() === 'DELETE')
  await confirmation.getByRole('button', { name: '댓글 삭제 확인' }).click()
  const deleted = await response
  expect(deleted.status()).toBe(204)
  expect(await deleted.text()).toBe('')
  await expect(list.getByRole('heading', { name: '댓글 0개' })).toBeVisible()
})

test('마감된 모집글의 새 댓글은 UI와 API에서 거부된다', async ({ page, request }) => {
  const owner = await register(request)
  const writer = await register(request, 'FE')
  const id = await createPost(request, owner.headers)
  const path = '/api' + detail(id)
  expect((await request.patch(path, {
    headers: owner.headers, data: { studyStatus: 'CLOSED' },
  })).status()).toBe(200)
  await loginUi(page, writer.account)
  await page.goto(detail(id))
  await expect(page.getByText('모집이 마감되어 새 댓글을 작성할 수 없습니다.')).toBeVisible()
  await expect(page.getByRole('button', { name: '댓글 등록' })).toHaveCount(0)
  await expectApiError(await request.post(path + '/comments', {
    headers: writer.headers, data: { content: '마감 뒤 신청' },
  }), 409, 'STUDY-003')
})

test('비로그인 조회는 가능하고 타인의 댓글 수정·삭제는 거부된다', async ({ page, request }) => {
  const owner = await register(request)
  const writer = await register(request, 'FE')
  const id = await createPost(request, owner.headers)
  const content = '작성자가 남긴 댓글'
  const commentId = await createComment(request, writer.headers, id, content)
  await page.goto(detail(id))
  await expect(page.getByText(content, { exact: true })).toBeVisible()
  await expect(page.getByText('댓글을 작성하려면', { exact: false })).toBeVisible()
  await loginUi(page, owner.account)
  await page.goto(detail(id))
  await expect(page.getByText(content, { exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: '댓글 수정' })).toHaveCount(0)
  await expect(page.getByRole('button', { name: '댓글 삭제', exact: true })).toHaveCount(0)
  const path = '/api/comments/' + commentId
  await expectApiError(await request.patch(path, { headers: owner.headers, data: { content: '권한 없는 수정' } }),
    403, 'COMMENT-002')
  await expectApiError(await request.delete(path, { headers: owner.headers }), 403, 'COMMENT-002')
  expect(await (await request.get('/api' + detail(id) + '/comments')).json()).toMatchObject({
    data: { totalElements: 1, content: [{ commentId, content }] },
  })
})
