import { test, expect, main, loginViaUI, uniqueId, authHeader, API_URL, type TestUser } from './fixtures'
import type { Page } from '@playwright/test'

const commentItems = (page: Page) => main(page).getByTestId('comment-item')
const commentSection = (page: Page) => main(page).getByRole('region', { name: /스터디 신청 댓글/ })

async function setup(data: import('./fixtures').TestData, opts: { closed?: boolean } = {}) {
    const author = await data.createUser('PM')
    const applicant = await data.createUser('FE')
    const postId = await data.createPost(author, { title: `E2E 댓글 ${uniqueId()}`, ...opts })
    return { author, applicant, postId }
}

async function writeComment(page: Page, content: string) {
    await main(page).getByLabel('신청 댓글 작성').fill(content)
    await main(page).getByRole('button', { name: '댓글 등록' }).click()
}

function expectAuthorInfo(item: ReturnType<typeof commentItems>, user: TestUser) {
    return Promise.all([
        expect(item).toContainText(user.nickname),
        expect(item).toContainText(`${user.generation}기`),
        expect(item).toContainText(user.part),
        expect(item.locator('time')).toHaveText(/\d{4}\.\d{2}\.\d{2} \d{2}:\d{2}/),
    ])
}

test.describe('C. 댓글 및 스터디 신청', () => {
    test('비로그인 사용자도 댓글 목록을 볼 수 있고 작성 대신 로그인을 안내받아요', async ({ page, data }) => {
        const { applicant, postId } = await setup(data)
        await data.createComment(applicant, postId, '비로그인 조회용 신청 댓글이에요.')

        await page.goto(`/studies/${postId}`)
        await expect(commentSection(page)).toContainText('스터디 신청 댓글1')
        await expect(commentItems(page)).toHaveCount(1)
        const item = commentItems(page).first()
        await expect(item).toContainText('비로그인 조회용 신청 댓글이에요.')
        await expectAuthorInfo(item, applicant)
        await expect(item.getByRole('button', { name: '수정' })).toHaveCount(0)
        await expect(item.getByRole('button', { name: '삭제' })).toHaveCount(0)

        await expect(main(page).getByLabel('신청 댓글 작성')).toHaveCount(0)
        await main(page).getByRole('link', { name: '로그인하고 신청하기' }).click()
        await expect(page).toHaveURL(/\/login$/)

        // 로그인하면 보던 모집글로 돌아와요.
        await loginViaUI(page, applicant, '')
        await expect(page).toHaveURL(new RegExp(`/studies/${postId}$`))
        await expect(main(page).getByLabel('신청 댓글 작성')).toBeVisible()
    })

    test('로그인 사용자가 댓글을 작성하면 목록에 바로 보여요 (1~500자 검증)', async ({ page, data }) => {
        const { applicant, postId } = await setup(data)
        await loginViaUI(page, applicant)
        await page.goto(`/studies/${postId}`)
        await expect(main(page).getByText('아직 신청 댓글이 없어요. 첫 번째로 신청해 보세요!')).toBeVisible()

        // 공백만 입력하면 막아요.
        await writeComment(page, '   ')
        await expect(commentSection(page).getByRole('alert')).toHaveText('댓글 내용을 입력해 주세요.')

        // 501자는 막아요.
        await writeComment(page, 'a'.repeat(501))
        await expect(commentSection(page).getByRole('alert')).toHaveText('댓글은 500자 이하로 입력해 주세요.')
        await expect(commentSection(page).getByText('501/500')).toBeVisible()
        await expect(commentItems(page)).toHaveCount(0)

        await writeComment(page, '안녕하세요! 스터디에 참여하고 싶어요.')
        await expect(commentItems(page)).toHaveCount(1)
        const item = commentItems(page).first()
        await expect(item).toContainText('안녕하세요! 스터디에 참여하고 싶어요.')
        await expect(item).toContainText('내 댓글')
        await expectAuthorInfo(item, applicant)
        await expect(main(page).getByLabel('신청 댓글 작성')).toHaveValue('')
        await expect(commentSection(page)).toContainText('스터디 신청 댓글1')

        // 500자는 작성돼요.
        await writeComment(page, 'b'.repeat(500))
        await expect(commentItems(page)).toHaveCount(2)
    })

    test('작성자는 자기 댓글을 수정할 수 있어요', async ({ page, data }) => {
        const { applicant, postId } = await setup(data)
        await data.createComment(applicant, postId, '수정 전 댓글')
        await loginViaUI(page, applicant)
        await page.goto(`/studies/${postId}`)

        const item = commentItems(page).first()
        await item.getByRole('button', { name: '수정' }).click()
        const editor = item.getByLabel('댓글 수정')
        await expect(editor).toHaveValue('수정 전 댓글')

        // 취소하면 원래 내용으로 돌아가요.
        await editor.fill('저장하지 않을 내용')
        await item.getByRole('button', { name: '취소' }).click()
        await expect(item).toContainText('수정 전 댓글')

        await item.getByRole('button', { name: '수정' }).click()
        await item.getByLabel('댓글 수정').fill('')
        await item.getByRole('button', { name: '저장' }).click()
        await expect(item.getByRole('alert')).toHaveText('댓글 내용을 입력해 주세요.')

        await item.getByLabel('댓글 수정').fill('수정된 신청 댓글이에요.')
        await item.getByRole('button', { name: '저장' }).click()
        await expect(item.getByLabel('댓글 수정')).toHaveCount(0)
        await expect(item).toContainText('수정된 신청 댓글이에요.')

        await page.reload()
        await expect(commentItems(page).first()).toContainText('수정된 신청 댓글이에요.')
    })

    test('작성자는 확인 후 자기 댓글을 삭제할 수 있어요', async ({ page, data }) => {
        const { applicant, postId } = await setup(data)
        await data.createComment(applicant, postId, '삭제할 댓글')
        await loginViaUI(page, applicant)
        await page.goto(`/studies/${postId}`)
        const item = commentItems(page).first()

        page.once('dialog', (dialog) => dialog.dismiss())
        await item.getByRole('button', { name: '삭제' }).click()
        await expect(commentItems(page)).toHaveCount(1)

        page.once('dialog', (dialog) => {
            expect(dialog.message()).toContain('이 댓글을 삭제할까요?')
            void dialog.accept()
        })
        await item.getByRole('button', { name: '삭제' }).click()
        await expect(commentItems(page)).toHaveCount(0)
        await expect(main(page).getByText('아직 신청 댓글이 없어요. 첫 번째로 신청해 보세요!')).toBeVisible()
        await expect(commentSection(page)).toContainText('스터디 신청 댓글0')
    })

    test('다른 사용자는 남의 댓글을 수정·삭제할 수 없어요 (모집글 작성자 포함, 화면과 서버 모두)', async ({
        page,
        data,
        request,
    }) => {
        const { author, applicant, postId } = await setup(data)
        const commentId = await data.createComment(applicant, postId, '남이 쓴 신청 댓글')
        await data.createComment(author, postId, '모집글 작성자의 댓글')

        await loginViaUI(page, author)
        await page.goto(`/studies/${postId}`)
        const [others, mine] = [commentItems(page).nth(0), commentItems(page).nth(1)]
        await expect(others).toContainText('남이 쓴 신청 댓글')
        await expect(others).not.toContainText('내 댓글')
        await expect(others.getByRole('button', { name: '수정' })).toHaveCount(0)
        await expect(others.getByRole('button', { name: '삭제' })).toHaveCount(0)
        // 자기 댓글에는 버튼이 보여요.
        await expect(mine.getByRole('button', { name: '수정' })).toBeVisible()

        // 서버도 막아요. (403 NOT_COMMENT_AUTHOR)
        const patch = await request.patch(`${API_URL}/comments/${commentId}`, {
            headers: authHeader(author.token),
            data: { content: '수정 시도' },
        })
        expect(patch.status()).toBe(403)
        expect((await patch.json()).code).toBe('COMMENT-002')
        const del = await request.delete(`${API_URL}/comments/${commentId}`, { headers: authHeader(author.token) })
        expect(del.status()).toBe(403)

        await page.reload()
        await expect(commentItems(page).nth(0)).toContainText('남이 쓴 신청 댓글')
    })

    test('모집이 마감된 글에는 댓글을 작성할 수 없어요', async ({ page, data }) => {
        const { applicant, postId } = await setup(data, { closed: true })
        await loginViaUI(page, applicant)
        await page.goto(`/studies/${postId}`)

        await expect(main(page).locator('article')).toContainText('모집 마감')
        await expect(main(page).getByText('모집이 마감된 스터디라 댓글을 작성할 수 없어요.')).toBeVisible()
        await expect(main(page).getByLabel('신청 댓글 작성')).toHaveCount(0)
        await expect(main(page).getByText('신청 댓글 없이 모집이 마감되었어요.')).toBeVisible()
    })

    test('화면을 연 뒤 모집이 마감되면 서버 409를 받아 작성을 막아요', async ({ page, data }) => {
        const { author, applicant, postId } = await setup(data)
        await loginViaUI(page, applicant)
        await page.goto(`/studies/${postId}`)
        await expect(main(page).getByLabel('신청 댓글 작성')).toBeVisible()

        // 다른 곳에서 작성자가 마감했어요.
        await data.closePost(author, postId)

        const response = page.waitForResponse(
            (res) => res.url().endsWith(`/api/study-posts/${postId}/comments`) && res.request().method() === 'POST',
        )
        await writeComment(page, '늦은 신청이에요.')
        expect((await response).status()).toBe(409)

        await expect(main(page).getByText('모집이 마감된 스터디라 댓글을 작성할 수 없어요.')).toBeVisible()
        await expect(main(page).getByText('방금 모집이 마감되었어요.', { exact: false })).toBeVisible()
        await expect(main(page).getByLabel('신청 댓글 작성')).toHaveCount(0)
        await expect(commentItems(page)).toHaveCount(0)
    })

    test('댓글을 오래된 순으로 20개씩 페이지로 나눠서 보여줘요', async ({ page, data }) => {
        const { applicant, postId } = await setup(data)
        for (let i = 1; i <= 21; i += 1) {
            await data.createComment(applicant, postId, `페이지 댓글 ${String(i).padStart(2, '0')}`)
        }

        await page.goto(`/studies/${postId}`)
        await expect(commentSection(page)).toContainText('스터디 신청 댓글21')
        await expect(commentItems(page)).toHaveCount(20)
        await expect(commentItems(page).first()).toContainText('페이지 댓글 01')
        await expect(commentItems(page).last()).toContainText('페이지 댓글 20')

        const pagination = commentSection(page).getByRole('navigation', { name: '페이지 이동' })
        await pagination.getByRole('button', { name: '2', exact: true }).click()
        await expect(commentItems(page)).toHaveCount(1)
        await expect(commentItems(page).first()).toContainText('페이지 댓글 21')
        await expect(pagination.getByRole('button', { name: '다음' })).toBeDisabled()

        // 첫 페이지에서 새 댓글을 쓰면 마지막 페이지로 이동해서 보여줘요.
        await pagination.getByRole('button', { name: '1', exact: true }).click()
        await expect(commentItems(page)).toHaveCount(20)
        await loginViaUI(page, applicant)
        await page.goto(`/studies/${postId}`)
        await writeComment(page, '페이지 댓글 22')
        await expect(commentItems(page)).toHaveCount(2)
        await expect(commentItems(page).last()).toContainText('페이지 댓글 22')
        await expect(pagination.getByRole('button', { name: '2', exact: true })).toHaveAttribute('aria-current', 'page')
    })
})
