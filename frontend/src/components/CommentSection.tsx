import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { createComment, deleteComment, fetchComments, updateComment } from '../api/comments'
import type { CommentItem, PageResponse } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { formatAuthor, formatDateTime } from '../utils/format'
import { getErrorMessage } from '../utils/messages'
import { TextAreaField } from './FormFields'
import { primaryButtonClass } from './styles'

const PAGE_SIZE = 20
const MAX_LENGTH = 500

// 서버 검증 규칙(docs/api-spec.md)과 같은 기준으로 먼저 확인한다.
function validateContent(content: string): string | null {
  if (content.trim().length < 1 || content.length > MAX_LENGTH) {
    return `댓글은 1자 이상 ${MAX_LENGTH}자 이하로 입력해 주세요.`
  }
  return null
}

interface CommentRowProps {
  comment: CommentItem
  canManage: boolean
  onChanged: () => void
  onDeleted: () => void
}

// 댓글 한 개. 작성자 본인에게만 수정·삭제 버튼을 보여준다.
function CommentRow({ comment, canManage, onChanged, onDeleted }: CommentRowProps) {
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState(comment.content)
  const [confirmingDelete, setConfirmingDelete] = useState(false)
  const [busy, setBusy] = useState(false)
  const [fieldError, setFieldError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  async function handleSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const invalid = validateContent(draft)
    setFieldError(invalid)
    if (invalid) return

    setBusy(true)
    setActionError(null)
    try {
      await updateComment(comment.commentId, { content: draft })
      setEditing(false)
      onChanged()
    } catch (err) {
      setActionError(getErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  async function handleDelete() {
    setBusy(true)
    setActionError(null)
    try {
      await deleteComment(comment.commentId)
      onDeleted()
    } catch (err) {
      setActionError(getErrorMessage(err))
      setConfirmingDelete(false)
      setBusy(false)
    }
  }

  function startEditing() {
    setDraft(comment.content)
    setFieldError(null)
    setActionError(null)
    setEditing(true)
  }

  return (
    <li className="rounded-md border border-slate-200 bg-white p-4">
      <p className="mb-2 text-sm text-slate-500">
        {formatAuthor(comment.author)} · {formatDateTime(comment.createdAt)}
      </p>

      {editing ? (
        <form onSubmit={handleSave} noValidate className="space-y-2">
          <TextAreaField
            label="댓글 수정"
            name="content"
            rows={3}
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            error={fieldError ?? undefined}
          />
          <div className="flex gap-2">
            <button type="submit" disabled={busy} className="rounded-md bg-indigo-600 px-3 py-1.5 text-sm font-semibold text-white disabled:opacity-50">
              저장
            </button>
            <button
              type="button"
              disabled={busy}
              onClick={() => setEditing(false)}
              className="rounded-md border border-slate-300 bg-white px-3 py-1.5 text-sm"
            >
              취소
            </button>
          </div>
        </form>
      ) : (
        <p className="whitespace-pre-wrap break-words text-slate-800">{comment.content}</p>
      )}

      {canManage && !editing && (
        <div className="mt-3 space-y-2">
          {!confirmingDelete && (
            <div className="flex gap-3 text-sm">
              <button
                type="button"
                aria-label="댓글 수정"
                disabled={busy}
                onClick={startEditing}
                className="text-slate-500 hover:text-indigo-600"
              >
                수정
              </button>
              <button
                type="button"
                aria-label="댓글 삭제"
                disabled={busy}
                onClick={() => setConfirmingDelete(true)}
                className="text-red-500 hover:text-red-700"
              >
                삭제
              </button>
            </div>
          )}

          {confirmingDelete && (
            <div role="alertdialog" aria-label="댓글 삭제 확인" className="rounded-md border border-red-200 bg-red-50 p-3">
              <p className="mb-2 text-sm text-red-700">이 댓글을 삭제할까요?</p>
              <div className="flex gap-2">
                <button
                  type="button"
                  aria-label="댓글 삭제 확인"
                  disabled={busy}
                  onClick={handleDelete}
                  className="rounded-md bg-red-600 px-3 py-1.5 text-sm font-semibold text-white disabled:opacity-50"
                >
                  삭제 확인
                </button>
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => setConfirmingDelete(false)}
                  className="rounded-md border border-slate-300 bg-white px-3 py-1.5 text-sm"
                >
                  취소
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {actionError && (
        <p role="alert" className="mt-2 text-sm text-red-600">
          {actionError}
        </p>
      )}
    </li>
  )
}

interface LoadedComments {
  key: string
  data: PageResponse<CommentItem> | null
  error: string | null
}

interface CommentSectionProps {
  postId: number
  // 모집이 마감된 글에는 댓글(스터디 신청)을 작성할 수 없다
  closed: boolean
}

export function CommentSection({ postId, closed }: CommentSectionProps) {
  const { isAuthenticated } = useAuth()
  const [page, setPage] = useState(0)
  // 댓글을 작성·수정·삭제한 뒤 목록을 다시 불러오기 위한 카운터
  const [reloadCount, setReloadCount] = useState(0)

  const [content, setContent] = useState('')
  const [fieldError, setFieldError] = useState<string | null>(null)
  const [serverError, setServerError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  // 로딩 여부는 요청 조건(key)과 도착한 결과의 key를 비교해 렌더링 중에 계산한다.
  // 로그인 상태가 바뀌면 key가 바뀌어 isAuthor를 서버에서 다시 받아온다.
  const requestKey = JSON.stringify([postId, page, isAuthenticated, reloadCount])
  const [loaded, setLoaded] = useState<LoadedComments | null>(null)

  useEffect(() => {
    let cancelled = false
    fetchComments(postId, page, PAGE_SIZE)
      .then((data) => {
        if (!cancelled) setLoaded({ key: requestKey, data, error: null })
      })
      .catch((err) => {
        if (!cancelled) setLoaded({ key: requestKey, data: null, error: getErrorMessage(err) })
      })
    return () => {
      cancelled = true
    }
  }, [postId, page, requestKey])

  const current = loaded?.key === requestKey ? loaded : null
  const loading = current === null
  const error = current?.error ?? null
  // 로딩 중에는 이전 결과를 그대로 보여줘서 화면이 깜빡이지 않게 한다
  const data = current ? current.data : loaded?.data
  const comments = data?.content ?? []
  const totalElements = data?.totalElements ?? 0
  const totalPages = data?.totalPages ?? 0

  function reload() {
    setReloadCount((count) => count + 1)
  }

  function handleDeleted() {
    // 마지막 페이지의 마지막 댓글을 지우면 이전 페이지로 돌아간다
    if (comments.length === 1 && page > 0) {
      setPage(page - 1)
    }
    reload()
  }

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setServerError(null)

    const invalid = validateContent(content)
    setFieldError(invalid)
    if (invalid) return

    setSubmitting(true)
    try {
      await createComment(postId, { content })
      setContent('')
      // 오래된 순이라 새 댓글은 마지막 페이지에 붙는다
      setPage(Math.max(Math.ceil((totalElements + 1) / PAGE_SIZE) - 1, 0))
      reload()
    } catch (err) {
      setServerError(getErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section aria-label="댓글" className="mt-8">
      <h2 className="mb-4 text-lg font-bold">댓글 ({totalElements})</h2>

      {error && (
        <p role="alert" className="mb-4 text-sm text-red-600">
          {error}
        </p>
      )}

      {loading && comments.length === 0 && <p className="py-4 text-center text-slate-500">불러오는 중이에요...</p>}

      {!loading && !error && comments.length === 0 && (
        <p className="py-4 text-center text-slate-500">아직 댓글이 없어요. 첫 신청 댓글을 남겨 보세요.</p>
      )}

      <ul className="space-y-3">
        {comments.map((comment) => (
          <CommentRow
            key={comment.commentId}
            comment={comment}
            canManage={comment.isAuthor && isAuthenticated}
            onChanged={reload}
            onDeleted={handleDeleted}
          />
        ))}
      </ul>

      {totalPages > 1 && (
        <nav aria-label="댓글 페이지 이동" className="mt-4 flex items-center justify-center gap-4 text-sm">
          <button
            type="button"
            disabled={page <= 0}
            onClick={() => setPage(page - 1)}
            className="rounded-md border border-slate-300 bg-white px-3 py-1.5 disabled:cursor-not-allowed disabled:opacity-40"
          >
            이전
          </button>
          <span className="text-slate-600">
            {page + 1} / {totalPages}
          </span>
          <button
            type="button"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage(page + 1)}
            className="rounded-md border border-slate-300 bg-white px-3 py-1.5 disabled:cursor-not-allowed disabled:opacity-40"
          >
            다음
          </button>
        </nav>
      )}

      <div className="mt-6">
        {!isAuthenticated && (
          <p className="text-sm text-slate-600">
            <Link to="/login" className="text-indigo-600 hover:underline">
              로그인
            </Link>
            하면 댓글로 스터디를 신청할 수 있어요.
          </p>
        )}

        {isAuthenticated && closed && (
          <p role="status" className="rounded-md bg-slate-100 px-3 py-2 text-sm text-slate-600">
            모집이 마감되어 댓글을 작성할 수 없어요.
          </p>
        )}

        {isAuthenticated && !closed && (
          <form onSubmit={handleCreate} noValidate className="space-y-3">
            <TextAreaField
              label="댓글 내용"
              name="content"
              rows={3}
              placeholder="스터디에 참여하고 싶은 이유를 남겨 주세요."
              value={content}
              onChange={(event) => setContent(event.target.value)}
              error={fieldError ?? undefined}
            />
            {serverError && (
              <p role="alert" className="text-sm text-red-600">
                {serverError}
              </p>
            )}
            <button type="submit" disabled={submitting} className={primaryButtonClass}>
              {submitting ? '등록 중...' : '댓글 등록'}
            </button>
          </form>
        )}
      </div>
    </section>
  )
}
