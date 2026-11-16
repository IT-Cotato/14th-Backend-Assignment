import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { getErrorCode, getErrorMessage } from '../api/client'
import { COMMENT_MAX, createComment, deleteComment, getComments, updateComment } from '../api/comments'
import type { Comment } from '../api/comments'
import { formatDateTime } from '../api/studyPosts'
import type { PageResponse, StudyStatus } from '../api/studyPosts'
import { useAuth } from '../auth/AuthContext'
import { ErrorMessage } from './ErrorMessage'
import { FormField } from './FormField'
import { cardClass, inputClass, submitClass } from './styles'

const PAGE_SIZE = 20
const CLOSED_CODE = 'STUDY-003'
const CLOSED_NOTICE = '모집이 마감된 글에는 댓글을 작성할 수 없습니다.'

const smallButtonClass =
  'rounded-md border px-3 py-1.5 text-sm font-medium disabled:cursor-not-allowed disabled:opacity-50'

const pageButtonClass =
  'rounded-md border border-gray-300 bg-white px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50'

interface ListResult {
  key: string
  data: PageResponse<Comment> | null
  error: string | null
}

interface CommentSectionProps {
  postId: number
  studyStatus: StudyStatus
}

export function CommentSection({ postId, studyStatus }: CommentSectionProps) {
  const { user, initializing } = useAuth()

  // page는 0부터 (API와 같음)
  const [page, setPage] = useState(0)
  const [reloadCount, setReloadCount] = useState(0)
  // 로그인 사용자가 바뀌면 isAuthor가 달라지므로 다시 조회한다
  const requestKey = `${postId}:${user?.userId ?? 'guest'}:${page}:${reloadCount}`
  const [result, setResult] = useState<ListResult | null>(null)

  // 작성 폼
  const [content, setContent] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [createError, setCreateError] = useState<string | null>(null)
  // 작성 중 마감돼서 409를 받았을 때의 서버 메시지
  const [closedMessage, setClosedMessage] = useState<string | null>(null)

  // 수정 / 삭제 (한 번에 하나의 댓글만)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editContent, setEditContent] = useState('')
  const [busy, setBusy] = useState<{ id: number; action: 'save' | 'delete' } | null>(null)
  const [itemError, setItemError] = useState<{ id: number; message: string } | null>(null)

  useEffect(() => {
    let ignore = false
    getComments(postId, page, PAGE_SIZE)
      .then((data) => {
        if (!ignore) setResult({ key: requestKey, data, error: null })
      })
      .catch((err) => {
        if (!ignore) setResult({ key: requestKey, data: null, error: getErrorMessage(err) })
      })
    return () => {
      ignore = true
    }
  }, [postId, page, requestKey])

  // 다시 조회하는 동안에는 이전 목록을 그대로 보여준다 (깜빡임 방지)
  const loading = result?.key !== requestKey
  const data = result?.data ?? null
  const loadError = !loading ? (result?.error ?? null) : null
  const totalElements = data?.totalElements ?? 0
  const totalPages = data?.totalPages ?? 0

  const reload = () => setReloadCount((count) => count + 1)

  const closed = studyStatus === 'CLOSED' || closedMessage !== null

  const handleCreate = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setSubmitting(true)
    setCreateError(null)
    try {
      await createComment(postId, content)
      setContent('')
      // 오래된 순이라 새 댓글은 마지막 페이지에 있다
      setPage(Math.floor(totalElements / PAGE_SIZE))
      reload()
    } catch (err) {
      if (getErrorCode(err) === CLOSED_CODE) {
        setClosedMessage(getErrorMessage(err))
      } else {
        setCreateError(getErrorMessage(err))
      }
    } finally {
      setSubmitting(false)
    }
  }

  const startEdit = (comment: Comment) => {
    setEditingId(comment.commentId)
    setEditContent(comment.content)
    setItemError(null)
  }

  const cancelEdit = () => {
    setEditingId(null)
    setItemError(null)
  }

  const handleUpdate = async (e: FormEvent<HTMLFormElement>, commentId: number) => {
    e.preventDefault()
    setBusy({ id: commentId, action: 'save' })
    setItemError(null)
    try {
      await updateComment(commentId, editContent)
      setEditingId(null)
      reload()
    } catch (err) {
      setItemError({ id: commentId, message: getErrorMessage(err) })
    } finally {
      setBusy(null)
    }
  }

  const handleDelete = async (commentId: number) => {
    if (!window.confirm('이 댓글을 삭제할까요?')) return
    setBusy({ id: commentId, action: 'delete' })
    setItemError(null)
    try {
      await deleteComment(commentId)
      if (editingId === commentId) setEditingId(null)
      // 지금 페이지의 마지막 댓글을 지웠으면 이전 페이지로
      if (page > 0 && data?.content.length === 1) setPage(page - 1)
      reload()
    } catch (err) {
      setItemError({ id: commentId, message: getErrorMessage(err) })
    } finally {
      setBusy(null)
    }
  }

  const renderForm = () => {
    if (initializing) return null
    if (closed) {
      return (
        <p role="status"
          className="rounded-md border border-gray-200 bg-gray-50 px-3 py-3 text-center text-sm text-gray-600">
          {closedMessage ?? CLOSED_NOTICE}
        </p>
      )
    }
    if (!user) {
      return (
        <div className="rounded-md border border-dashed border-gray-300 px-3 py-4 text-center text-sm text-gray-600">
          스터디에 참여하고 싶다면{' '}
          <Link to="/login" className="font-medium text-indigo-600 hover:underline">
            로그인하고 신청하기
          </Link>
        </div>
      )
    }
    return (
      <form noValidate onSubmit={handleCreate} className="flex flex-col gap-3">
        <FormField id="comment-content" label="신청 댓글">
          <textarea id="comment-content" name="content" rows={3} maxLength={COMMENT_MAX}
            aria-describedby="comment-content-count" placeholder="자기소개나 참여 의사를 남겨주세요."
            value={content} onChange={(e) => setContent(e.target.value)}
            className={`${inputClass} resize-y`} />
          <p id="comment-content-count" className="text-right text-xs text-gray-500">
            {content.length} / {COMMENT_MAX}
          </p>
        </FormField>
        <ErrorMessage message={createError} />
        <div className="flex justify-end">
          <button type="submit" disabled={submitting} className={`${submitClass} sm:w-32`}>
            {submitting ? '등록 중...' : '등록'}
          </button>
        </div>
      </form>
    )
  }

  return (
    <section aria-labelledby="comments-title" className={`${cardClass} flex flex-col gap-4`}>
      <h2 id="comments-title" className="text-lg font-semibold text-gray-900">
        댓글 <span className="text-indigo-600">{data ? totalElements : ''}</span>
      </h2>

      {loading && !data && <p className="py-4 text-center text-gray-500">불러오는 중...</p>}
      <ErrorMessage message={loadError} />

      {data && totalElements === 0 && (
        <p className="rounded-md border border-dashed border-gray-300 py-6 text-center text-sm text-gray-500">
          아직 신청 댓글이 없습니다.
        </p>
      )}

      {data && data.content.length > 0 && (
        <ul aria-busy={loading} className={`flex flex-col divide-y divide-gray-100 ${loading ? 'opacity-60' : ''}`}>
          {data.content.map((comment) => {
            const editing = comment.isAuthor && editingId === comment.commentId
            const itemBusy = busy?.id === comment.commentId
            const error = itemError?.id === comment.commentId ? itemError.message : null
            const editId = `comment-edit-${comment.commentId}`

            return (
              <li key={comment.commentId} className="flex flex-col gap-2 py-3 first:pt-0 last:pb-0">
                <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm">
                  <span className="font-medium text-gray-900">{comment.author.nickname}</span>
                  <span className="text-gray-500">
                    {comment.author.generation}기 · {comment.author.part}
                  </span>
                  <span className="text-gray-400">{formatDateTime(comment.createdAt)}</span>
                </div>

                {editing ? (
                  <form noValidate onSubmit={(e) => handleUpdate(e, comment.commentId)}
                    className="flex flex-col gap-2">
                    <FormField id={editId} label="댓글 수정">
                      <textarea id={editId} rows={3} maxLength={COMMENT_MAX}
                        aria-describedby={`${editId}-count`}
                        value={editContent} onChange={(e) => setEditContent(e.target.value)}
                        className={`${inputClass} resize-y`} />
                      <p id={`${editId}-count`} className="text-right text-xs text-gray-500">
                        {editContent.length} / {COMMENT_MAX}
                      </p>
                    </FormField>
                    <ErrorMessage message={error} />
                    <div className="flex flex-wrap justify-end gap-2">
                      <button type="button" onClick={cancelEdit} disabled={itemBusy}
                        className={`${smallButtonClass} border-gray-300 text-gray-700 hover:bg-gray-50`}>
                        취소
                      </button>
                      <button type="submit" disabled={itemBusy}
                        className={`${smallButtonClass} border-indigo-600 bg-indigo-600 text-white hover:bg-indigo-700`}>
                        {itemBusy && busy?.action === 'save' ? '저장 중...' : '저장'}
                      </button>
                    </div>
                  </form>
                ) : (
                  <>
                    <p className="whitespace-pre-wrap break-words text-gray-800">{comment.content}</p>
                    {comment.isAuthor && (
                      <>
                        <ErrorMessage message={error} />
                        <div className="flex flex-wrap justify-end gap-2">
                          <button type="button" onClick={() => startEdit(comment)} disabled={busy !== null}
                            className={`${smallButtonClass} border-gray-300 text-gray-700 hover:bg-gray-50`}>
                            수정
                          </button>
                          <button type="button" onClick={() => handleDelete(comment.commentId)}
                            disabled={busy !== null}
                            className={`${smallButtonClass} border-red-300 text-red-700 hover:bg-red-50`}>
                            {itemBusy && busy?.action === 'delete' ? '삭제 중...' : '삭제'}
                          </button>
                        </div>
                      </>
                    )}
                  </>
                )}
              </li>
            )
          })}
        </ul>
      )}

      {data && (totalPages > 1 || page > 0) && (
        <nav aria-label="댓글 페이지 이동" className="flex items-center justify-center gap-3">
          <button type="button" onClick={() => setPage(page - 1)} disabled={page <= 0 || loading}
            className={pageButtonClass}>
            이전
          </button>
          <span className="text-sm text-gray-700">
            {page + 1} / {Math.max(totalPages, page + 1)}
          </span>
          <button type="button" onClick={() => setPage(page + 1)}
            disabled={page + 1 >= totalPages || loading} className={pageButtonClass}>
            다음
          </button>
        </nav>
      )}

      <div className="border-t border-gray-100 pt-4">{renderForm()}</div>
    </section>
  )
}
