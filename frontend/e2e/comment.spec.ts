import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'
import {
  API_URL,
  authHeaders,
  createUserWithToken,
  expectApiError,
  loginInBrowser,
} from './helpers/auth'
import { closePostViaApi, createCommentViaApi, createPostViaApi } from './helpers/studyPost'
import { uniqueSuffix } from './helpers/user'

// 상세 화면을 열고 로그인 상태가 반영될 때까지 기다린 뒤 댓글 영역을 돌려준다
async function openPost(page: Page, postId: number, nickname: string) {
  await page.goto(`/studies/${postId}`)
  await expect(
    page.getByRole('navigation', { name: '사용자 메뉴' }).getByText(nickname, { exact: true }),
  ).toBeVisible()
  return page.getByRole('region', { name: /댓글/ })
}

test.describe('댓글(스터디 신청)', () => {
  test('댓글 작성 → 수정 → 삭제', async ({ page, request }) => {
    const author = await createUserWithToken(request)
    const post = await createPostViaApi(request, author.token)
    const applicant = await createUserWithToken(request)
    await loginInBrowser(page, applicant.token)
    const content = `참여하고 싶어요 ${uniqueSuffix(8)}`
    const editedContent = `수정한 신청 댓글 ${uniqueSuffix(8)}`

    const comments = await openPost(page, post.postId, applicant.user.nickname)

    await test.step('작성', async () => {
      await comments.getByLabel('신청 댓글', { exact: true }).fill(content)
      await comments.getByRole('button', { name: '등록', exact: true }).click()

      await expect(comments.getByText(content)).toBeVisible()
      await expect(comments.getByText(applicant.user.nickname, { exact: true })).toBeVisible()
    })

    await test.step('수정', async () => {
      await comments.getByRole('button', { name: '수정', exact: true }).click()
      const editBox = comments.getByLabel('댓글 수정', { exact: true })
      await expect(editBox).toHaveValue(content)
      await editBox.fill(editedContent)
      await comments.getByRole('button', { name: '저장', exact: true }).click()

      await expect(comments.getByText(editedContent)).toBeVisible()
      await expect(comments.getByText(content)).toHaveCount(0)
    })

    await test.step('삭제', async () => {
      page.once('dialog', (dialog) => dialog.accept())
      await comments.getByRole('button', { name: '삭제', exact: true }).click()

      await expect(comments.getByText('아직 신청 댓글이 없습니다.')).toBeVisible()
      await expect(comments.getByText(editedContent)).toHaveCount(0)
    })
  })

  test('마감된 글에는 댓글을 작성할 수 없다', async ({ page, request }) => {
    const author = await createUserWithToken(request)
    const post = await createPostViaApi(request, author.token)
    await closePostViaApi(request, author.token, post.postId)
    const applicant = await createUserWithToken(request)
    await loginInBrowser(page, applicant.token)

    const comments = await openPost(page, post.postId, applicant.user.nickname)

    await expect(comments.getByRole('status')).toHaveText(
      '모집이 마감된 글에는 댓글을 작성할 수 없습니다.',
    )
    await expect(comments.getByLabel('신청 댓글', { exact: true })).toHaveCount(0)

    const response = await request.post(`${API_URL}/api/study-posts/${post.postId}/comments`, {
      headers: authHeaders(applicant.token),
      data: { content: `마감 후 신청 ${uniqueSuffix(8)}` },
    })
    await expectApiError(response, 409, 'STUDY-003')
  })

  test('남의 댓글은 수정할 수 없다', async ({ page, request }) => {
    const author = await createUserWithToken(request)
    const post = await createPostViaApi(request, author.token)
    const commenter = await createUserWithToken(request)
    const comment = await createCommentViaApi(request, commenter.token, post.postId)
    const other = await createUserWithToken(request)
    await loginInBrowser(page, other.token)

    const comments = await openPost(page, post.postId, other.user.nickname)

    await expect(comments.getByText(comment.content)).toBeVisible()
    await expect(comments.getByRole('button', { name: '수정', exact: true })).toHaveCount(0)
    await expect(comments.getByRole('button', { name: '삭제', exact: true })).toHaveCount(0)

    const updateResponse = await request.patch(`${API_URL}/api/comments/${comment.commentId}`, {
      headers: authHeaders(other.token),
      data: { content: '남이 바꾼 댓글' },
    })
    await expectApiError(updateResponse, 403, 'COMMENT-002')

    const deleteResponse = await request.delete(`${API_URL}/api/comments/${comment.commentId}`, {
      headers: authHeaders(other.token),
    })
    await expectApiError(deleteResponse, 403, 'COMMENT-002')
  })
})
