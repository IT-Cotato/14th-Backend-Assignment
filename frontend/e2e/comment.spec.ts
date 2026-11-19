import { expect, test } from '@playwright/test'
import { auth, createPostByApi, createUser, loginByUi } from './helpers'

test.describe('댓글(스터디 신청)', () => {
  test('댓글 작성 → 수정 → 삭제', async ({ page, request }) => {
    const owner = await createUser(request)
    const applicant = await createUser(request, { part: 'FE', generation: 13 })
    const { postId } = await createPostByApi(request, owner.token)

    await loginByUi(page, applicant.user)
    await page.goto(`/studies/${postId}`)

    // 작성
    await page.getByLabel('신청 댓글 작성', { exact: true }).fill('알고리즘 입문 중인데 참여하고 싶어요.')
    await page.getByRole('button', { name: '신청하기' }).click()
    const comment = page.getByRole('listitem').filter({ hasText: '알고리즘 입문 중인데 참여하고 싶어요.' })
    await expect(comment).toBeVisible()
    await expect(comment).toContainText(applicant.user.nickname)
    await expect(comment).toContainText('13기')

    // 수정
    await comment.getByRole('button', { name: '댓글 수정' }).click()
    await page.getByLabel('수정할 댓글').fill('수정한 신청 메시지')
    await page.getByRole('button', { name: '저장' }).click()
    await expect(page.getByText('수정한 신청 메시지')).toBeVisible()
    await expect(page.getByText('알고리즘 입문 중인데 참여하고 싶어요.')).toHaveCount(0)

    // 삭제
    page.once('dialog', (dialog) => dialog.accept())
    await page.getByRole('button', { name: '댓글 삭제' }).click()
    await expect(page.getByText('수정한 신청 메시지')).toHaveCount(0)
    await expect(page.getByText('아직 신청 댓글이 없어요.')).toBeVisible()
  })

  test('마감된 글에는 댓글 입력창이 없고, API로 써도 409가 온다', async ({ page, request }) => {
    const owner = await createUser(request)
    const applicant = await createUser(request)
    const { postId } = await createPostByApi(request, owner.token)
    const close = await request.patch(`/api/study-posts/${postId}`, {
      headers: auth(owner.token),
      data: { studyStatus: 'CLOSED' },
    })
    expect(close.status()).toBe(200)

    await loginByUi(page, applicant.user)
    await page.goto(`/studies/${postId}`)
    await expect(page.getByText('모집이 마감되어 댓글을 쓸 수 없어요.')).toBeVisible()
    await expect(page.getByLabel('신청 댓글 작성', { exact: true })).toHaveCount(0)

    const res = await request.post(`/api/study-posts/${postId}/comments`, {
      headers: auth(applicant.token),
      data: { content: '늦었지만 신청해요' },
    })
    expect(res.status()).toBe(409)
    expect((await res.json()).code).toBe('STUDY-003')
  })

  test('남의 댓글은 수정·삭제 버튼이 없고, API로 시도해도 403이 온다', async ({ page, request }) => {
    const owner = await createUser(request)
    const applicant = await createUser(request)
    const other = await createUser(request)
    const { postId } = await createPostByApi(request, owner.token)
    const created = await request.post(`/api/study-posts/${postId}/comments`, {
      headers: auth(applicant.token),
      data: { content: '신청합니다' },
    })
    const commentId = (await created.json()).data.commentId

    await loginByUi(page, other.user)
    await page.goto(`/studies/${postId}`)
    await expect(page.getByText('신청합니다')).toBeVisible()
    await expect(page.getByRole('button', { name: '댓글 수정' })).toHaveCount(0)
    await expect(page.getByRole('button', { name: '댓글 삭제' })).toHaveCount(0)

    const patch = await request.patch(`/api/comments/${commentId}`, {
      headers: auth(other.token),
      data: { content: '해킹' },
    })
    expect(patch.status()).toBe(403)
    expect((await patch.json()).code).toBe('COMMENT-002')

    const del = await request.delete(`/api/comments/${commentId}`, { headers: auth(other.token) })
    expect(del.status()).toBe(403)
  })
})
