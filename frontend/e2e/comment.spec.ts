import { randomUUID } from 'node:crypto'
import { expect, test } from '@playwright/test'
import {
  acceptNextDialog,
  API_URL,
  authHeader,
  closePostViaApi,
  createCommentViaApi,
  createPostViaApi,
  createUserViaApi,
  loginViaUi,
  uniqueTitle,
} from './helpers.ts'

/** 다른 테스트의 댓글과 겹치지 않는 내용을 만든다 */
function uniqueComment(prefix: string): string {
  return `${prefix} ${randomUUID().slice(0, 8)}`
}

test.describe('댓글(스터디 신청)', () => {
  test('모집글에 댓글을 작성하고 수정한 뒤 삭제할 수 있다', async ({ page, request }) => {
    const author = await createUserViaApi(request)
    const applicant = await createUserViaApi(request, { generation: 13, part: 'FE' })
    const postId = await createPostViaApi(request, author.token, {
      title: uniqueTitle('E2E 댓글'),
      content: '댓글로 신청해 주세요.',
    })
    const content = uniqueComment('참여하고 싶어요')
    const editedContent = uniqueComment('수정한 신청 메시지')
    const commentItem = (text: string) => page.getByRole('listitem').filter({ hasText: text })

    await loginViaUi(page, applicant.user)
    await page.goto(`/studies/${postId}`)

    // 작성
    await page.getByLabel('댓글 내용', { exact: true }).fill(content)
    await page.getByRole('button', { name: '댓글 작성' }).click()
    await expect(commentItem(content)).toBeVisible()
    await expect(commentItem(content)).toContainText(`${applicant.user.nickname} (13기 FE)`)
    await expect(page.getByLabel('댓글 내용', { exact: true })).toHaveValue('')

    // 수정
    await commentItem(content).getByRole('button', { name: '댓글 수정' }).click()
    await page.getByLabel('수정할 댓글 내용').fill(editedContent)
    await page.getByRole('button', { name: '댓글 저장' }).click()
    await expect(commentItem(editedContent)).toBeVisible()
    await expect(page.getByText(content)).toHaveCount(0)

    // 삭제
    acceptNextDialog(page)
    await commentItem(editedContent).getByRole('button', { name: '댓글 삭제' }).click()
    await expect(page.getByText(editedContent)).toHaveCount(0)
    await expect(page.getByText('아직 댓글이 없습니다.')).toBeVisible()
  })

  test('모집이 마감된 글에는 댓글 작성 폼이 없고 서버도 409로 막는다', async ({ page, request }) => {
    const author = await createUserViaApi(request)
    const applicant = await createUserViaApi(request)
    const title = uniqueTitle('E2E 마감 글')
    const postId = await createPostViaApi(request, author.token, {
      title,
      content: '이미 마감된 스터디',
    })
    await closePostViaApi(request, author.token, postId)

    await loginViaUi(page, applicant.user)
    await page.goto(`/studies/${postId}`)
    await expect(page.getByRole('heading', { name: title })).toBeVisible()
    await expect(page.getByText('모집이 마감되어 댓글을 작성할 수 없습니다.')).toBeVisible()
    await expect(page.getByRole('button', { name: '댓글 작성' })).toHaveCount(0)

    const response = await request.post(`${API_URL}/api/study-posts/${postId}/comments`, {
      headers: authHeader(applicant.token),
      data: { content: '마감된 글에 쓰는 댓글' },
    })
    expect(response.status()).toBe(409)
    expect((await response.json()).code).toBe('STUDY-003')
  })

  test('남의 댓글에는 수정, 삭제 버튼이 보이지 않고 서버도 403으로 막는다', async ({
    page,
    request,
  }) => {
    const author = await createUserViaApi(request)
    const other = await createUserViaApi(request)
    const postId = await createPostViaApi(request, author.token, {
      title: uniqueTitle('E2E 남의 댓글'),
      content: '댓글 권한 확인용 글',
    })
    const content = uniqueComment('작성자가 남긴 댓글')
    const commentId = await createCommentViaApi(request, author.token, postId, content)

    await loginViaUi(page, other.user)
    await page.goto(`/studies/${postId}`)
    await expect(page.getByText(content)).toBeVisible()
    await expect(page.getByRole('button', { name: '댓글 수정' })).toHaveCount(0)
    await expect(page.getByRole('button', { name: '댓글 삭제' })).toHaveCount(0)

    const patchResponse = await request.patch(`${API_URL}/api/comments/${commentId}`, {
      headers: authHeader(other.token),
      data: { content: '남이 바꾼 댓글' },
    })
    expect(patchResponse.status()).toBe(403)
    expect((await patchResponse.json()).code).toBe('COMMENT-002')

    const deleteResponse = await request.delete(`${API_URL}/api/comments/${commentId}`, {
      headers: authHeader(other.token),
    })
    expect(deleteResponse.status()).toBe(403)
    expect((await deleteResponse.json()).code).toBe('COMMENT-002')

    await page.reload()
    await expect(page.getByText(content)).toBeVisible()
  })

  test('로그인하지 않으면 댓글 작성 폼 대신 로그인 안내가 보이고 서버도 401로 막는다', async ({
    page,
    request,
  }) => {
    const author = await createUserViaApi(request)
    const postId = await createPostViaApi(request, author.token, {
      title: uniqueTitle('E2E 비로그인 댓글'),
      content: '로그인해야 신청할 수 있는 글',
    })

    await page.goto(`/studies/${postId}`)
    await expect(page.getByText('댓글을 작성하려면')).toBeVisible()
    await expect(page.getByRole('button', { name: '댓글 작성' })).toHaveCount(0)

    const response = await request.post(`${API_URL}/api/study-posts/${postId}/comments`, {
      data: { content: '로그인 없이 쓰는 댓글' },
    })
    expect(response.status()).toBe(401)
    expect((await response.json()).code).toBe('COMMON-004')
  })
})
