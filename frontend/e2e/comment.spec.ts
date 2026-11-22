import { expectError } from './support/api.ts'
import { newPostTitle, uniqueTag } from './support/data.ts'
import { expect, loginAs, test } from './support/fixtures.ts'

test.describe('댓글(= 스터디 신청)', () => {
  test('댓글을 작성하고, 그 자리에서 수정하고, 삭제해요', async ({ page, api, createUser, trackPost }) => {
    const author = await createUser()
    const applicant = await createUser()
    const title = newPostTitle(uniqueTag())
    const postId = await api.createPost(author.token, { title, content: '신청은 댓글로 받아요' })
    trackPost(postId, author.token)

    const content = `참여하고 싶어요 ${uniqueTag()}`
    const editedContent = `${content} (수정)`

    await loginAs(page, applicant.token)
    await page.goto(`/studies/${postId}`)
    const comments = page.getByRole('region', { name: '댓글' })
    await expect(comments.getByText('아직 신청 댓글이 없어요.')).toBeVisible()

    await test.step('작성하면 목록에 바로 보이고 개수가 늘어요', async () => {
      await comments.getByLabel('댓글 내용').fill(content)
      await comments.getByRole('button', { name: '댓글 등록', exact: true }).click()

      await expect(comments.getByRole('listitem').filter({ hasText: content })).toBeVisible()
      await expect(comments.getByText('총 1개')).toBeVisible()
      await expect(comments.getByLabel('댓글 내용')).toHaveValue('')
    })

    await test.step('수정 버튼을 누르면 그 자리에서 고칠 수 있어요', async () => {
      const item = comments.getByRole('listitem').filter({ hasText: content })
      await item.getByRole('button', { name: '댓글 수정', exact: true }).click()

      await expect(comments.getByLabel('수정할 내용')).toHaveValue(content)
      await comments.getByLabel('수정할 내용').fill(editedContent)
      await comments.getByRole('button', { name: '저장', exact: true }).click()

      await expect(comments.getByRole('listitem').filter({ hasText: editedContent })).toBeVisible()
      await expect(comments.getByLabel('수정할 내용')).toHaveCount(0)
    })

    await test.step('삭제를 확인하면 목록에서 사라져요', async () => {
      const item = comments.getByRole('listitem').filter({ hasText: editedContent })
      page.once('dialog', async (dialog) => {
        expect(dialog.type()).toBe('confirm')
        expect(dialog.message()).toContain('삭제할까요')
        await dialog.accept()
      })
      await item.getByRole('button', { name: '댓글 삭제', exact: true }).click()

      await expect(comments.getByText('아직 신청 댓글이 없어요.')).toBeVisible()
      await expect(comments.getByText('총 0개')).toBeVisible()
      expect((await api.getComments(postId)).totalElements).toBe(0)
    })
  })

  test('로그인하지 않으면 작성 폼 대신 로그인 안내가 보여요', async ({ page, api, createUser, trackPost }) => {
    const author = await createUser()
    const postId = await api.createPost(author.token, { title: newPostTitle(uniqueTag()), content: '내용' })
    trackPost(postId, author.token)

    await page.goto(`/studies/${postId}`)
    const comments = page.getByRole('region', { name: '댓글' })

    await expect(comments.getByText('로그인하면 신청할 수 있어요.')).toBeVisible()
    await expect(comments.getByRole('link', { name: '로그인하고 신청하기', exact: true })).toBeVisible()
    await expect(comments.getByLabel('댓글 내용')).toHaveCount(0)
  })

  test('마감된 글에는 댓글을 쓸 수 없어요', async ({ page, api, createUser, trackPost }) => {
    const author = await createUser()
    const applicant = await createUser()
    const postId = await api.createPost(author.token, { title: newPostTitle(uniqueTag()), content: '곧 마감해요' })
    trackPost(postId, author.token)
    expect((await api.updatePost(postId, author.token, { studyStatus: 'CLOSED' })).status()).toBe(200)

    await loginAs(page, applicant.token)

    await test.step('화면에는 작성 폼 대신 마감 안내가 보여요', async () => {
      await page.goto(`/studies/${postId}`)
      const comments = page.getByRole('region', { name: '댓글' })

      await expect(comments.getByText('모집이 마감됐어요. 더 이상 신청할 수 없어요.')).toBeVisible()
      await expect(comments.getByLabel('댓글 내용')).toHaveCount(0)
      await expect(comments.getByRole('button', { name: '댓글 등록' })).toHaveCount(0)
    })

    await test.step('API로 직접 작성해도 서버가 409로 막아요', async () => {
      await expectError(await api.createComment(postId, applicant.token, '늦었지만 신청해요'), 409, 'STUDY-003')
      expect((await api.getComments(postId)).totalElements).toBe(0)
    })
  })

  test('남의 댓글은 수정·삭제할 수 없어요', async ({ page, api, createUser, trackPost }) => {
    const author = await createUser()
    const applicant = await createUser()
    const other = await createUser()
    const postId = await api.createPost(author.token, { title: newPostTitle(uniqueTag()), content: '내용' })
    trackPost(postId, author.token)

    const content = `신청자의 댓글 ${uniqueTag()}`
    const created = await api.createComment(postId, applicant.token, content)
    expect(created.status()).toBe(201)
    const commentId: number = (await created.json()).data.commentId

    await loginAs(page, other.token)

    await test.step('남의 댓글에는 수정·삭제 버튼이 보이지 않아요', async () => {
      await page.goto(`/studies/${postId}`)
      const item = page.getByRole('region', { name: '댓글' }).getByRole('listitem').filter({ hasText: content })

      await expect(item).toBeVisible()
      await expect(item.getByRole('button', { name: '댓글 수정' })).toHaveCount(0)
      await expect(item.getByRole('button', { name: '댓글 삭제' })).toHaveCount(0)
    })

    await test.step('API로 직접 수정·삭제해도 서버가 403으로 막아요', async () => {
      await expectError(await api.updateComment(commentId, other.token, '남이 고친 내용'), 403, 'COMMENT-002')
      await expectError(await api.deleteComment(commentId, other.token), 403, 'COMMENT-002')

      const list = await api.getComments(postId)
      expect(list.totalElements).toBe(1)
      expect(list.content[0].content).toBe(content)
    })
  })
})
