import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'

import { createComment, deleteComment, getComments, updateComment } from '../api/comments'
import { getErrorMessage } from '../api/error'
import { partLabel, type Comment, type PageResponse } from '../api/types'
import { useAuth } from '../auth/authContext'
import { formatDateTime } from '../utils/format'
import { Pagination } from './Pagination'
import { ErrorMessage, Loading } from './StatusMessage'

const PAGE_SIZE = 20
const CONTENT_MAX = 500

const textareaClass =
  'mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-gray-900 focus:outline-none'

function validateContent(content: string): string | null {
  const trimmed = content.trim()
  if (!trimmed || trimmed.length > CONTENT_MAX) {
    return `댓글은 1~${CONTENT_MAX}자로 입력해 주세요.`
  }
  return null
}

interface CommentSectionProps {
  postId: number
  // 모집이 마감된 글이면 새 신청(댓글)을 받지 않아요
  isClosed: boolean
}

// 모집글 상세 아래의 댓글(= 스터디 신청) 영역
export function CommentSection({ postId, isClosed }: CommentSectionProps) {
  const { isLoggedIn } = useAuth()

  const [page, setPage] = useState(0)
  // 작성·수정·삭제 뒤에 목록을 다시 불러오려고 올리는 숫자
  const [reloadCount, setReloadCount] = useState(0)

  // 어떤 요청에 대한 응답인지 key로 함께 저장해요. key가 다르면 아직 불러오는 중이에요.
  const requestKey = `${postId}:${page}:${reloadCount}`
  const [loaded, setLoaded] = useState<{
    key: string
    result?: PageResponse<Comment>
    error?: string
  } | null>(null)

  useEffect(() => {
    let cancelled = false
    const key = `${postId}:${page}:${reloadCount}`
    getComments(postId, page, PAGE_SIZE)
      .then((data) => {
        if (!cancelled) setLoaded({ key, result: data })
      })
      .catch((err: unknown) => {
        if (!cancelled) setLoaded({ key, error: getErrorMessage(err) })
      })
    return () => {
      cancelled = true
    }
  }, [postId, page, reloadCount])

  const isLoading = loaded?.key !== requestKey
  // 다시 불러오는 동안에도 이전 목록을 보여줘서 화면이 깜빡이지 않게 해요
  const result = loaded?.result ?? null
  const loadError = isLoading ? null : (loaded?.error ?? null)

  const reload = () => setReloadCount((count) => count + 1)

  // ---------- 작성 ----------
  const [newContent, setNewContent] = useState('')
  const [createError, setCreateError] = useState<string | null>(null)
  const [isCreating, setIsCreating] = useState(false)

  const handleCreate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setCreateError(null)

    const validationError = validateContent(newContent)
    if (validationError) {
      setCreateError(validationError)
      return
    }

    setIsCreating(true)
    try {
      await createComment(postId, { content: newContent.trim() })
      setNewContent('')
      // 오래된 순이라 새 댓글은 마지막 페이지에 있어요. 그 페이지로 이동해요.
      const total = (result?.totalElements ?? 0) + 1
      const lastPage = Math.max(0, Math.ceil(total / PAGE_SIZE) - 1)
      setPage(lastPage)
      reload()
    } catch (err) {
      setCreateError(getErrorMessage(err))
    } finally {
      setIsCreating(false)
    }
  }

  return (
    <section aria-labelledby="comments-heading" className="rounded-lg border border-gray-200 bg-white p-5">
      <h2 id="comments-heading" className="mb-4 text-lg font-bold">
        신청 댓글 {result ? `${result.totalElements}개` : ''}
      </h2>

      {/* 작성 영역 */}
      {isClosed ? (
        <p className="mb-4 rounded-md bg-gray-100 px-3 py-2 text-sm text-gray-600">
          모집이 마감되어 더 이상 신청할 수 없어요.
        </p>
      ) : isLoggedIn ? (
        <form onSubmit={handleCreate} noValidate className="mb-6 space-y-2">
          {createError && <ErrorMessage message={createError} />}
          <label htmlFor="new-comment" className="text-sm font-medium">
            신청 메시지
          </label>
          <textarea
            id="new-comment"
            rows={3}
            value={newContent}
            maxLength={CONTENT_MAX}
            onChange={(e) => setNewContent(e.target.value)}
            placeholder="스터디에 참여하고 싶은 이유를 남겨 주세요."
            className={textareaClass}
          />
          <div className="flex items-center justify-between">
            <span className="text-xs text-gray-400">
              {newContent.length} / {CONTENT_MAX}
            </span>
            <button
              type="submit"
              disabled={isCreating}
              className="rounded-md bg-gray-900 px-4 py-2 text-sm font-medium text-white hover:bg-gray-700 disabled:opacity-50"
            >
              {isCreating ? '신청하는 중...' : '신청하기'}
            </button>
          </div>
        </form>
      ) : (
        <p className="mb-4 rounded-md bg-gray-100 px-3 py-2 text-sm text-gray-600">
          <Link to="/login" className="font-medium text-gray-900 underline">
            로그인
          </Link>
          하면 댓글로 스터디에 신청할 수 있어요.
        </p>
      )}

      {/* 목록 */}
      {loadError && <ErrorMessage message={loadError} />}
      {isLoading && !result && <Loading />}

      {result && (
        <>
          {result.content.length === 0 ? (
            <p className="py-4 text-center text-sm text-gray-500">아직 신청 댓글이 없어요.</p>
          ) : (
            <ul className="divide-y divide-gray-100">
              {result.content.map((comment) => (
                <CommentItem key={comment.commentId} comment={comment} onChanged={reload} />
              ))}
            </ul>
          )}
          <Pagination page={result.page} totalPages={result.totalPages} onChange={setPage} />
        </>
      )}
    </section>
  )
}

interface CommentItemProps {
  comment: Comment
  onChanged: () => void
}

// 댓글 하나. 내 댓글이면 수정·삭제 버튼이 보여요. (서버도 작성자가 아니면 403으로 막아요)
function CommentItem({ comment, onChanged }: CommentItemProps) {
  const [isEditing, setIsEditing] = useState(false)
  const [editContent, setEditContent] = useState(comment.content)
  const [isConfirmingDelete, setIsConfirmingDelete] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [isWorking, setIsWorking] = useState(false)

  const startEdit = () => {
    setEditContent(comment.content)
    setError(null)
    setIsEditing(true)
  }

  const handleUpdate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)

    const validationError = validateContent(editContent)
    if (validationError) {
      setError(validationError)
      return
    }

    setIsWorking(true)
    try {
      await updateComment(comment.commentId, { content: editContent.trim() })
      setIsEditing(false)
      onChanged()
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setIsWorking(false)
    }
  }

  const handleDelete = async () => {
    setError(null)
    setIsWorking(true)
    try {
      await deleteComment(comment.commentId)
      onChanged()
    } catch (err) {
      setError(getErrorMessage(err))
      setIsWorking(false)
    }
  }

  const editId = `edit-comment-${comment.commentId}`

  return (
    <li className="py-3">
      <p className="mb-1 flex flex-wrap gap-x-3 gap-y-1 text-xs text-gray-500">
        <span className="font-medium text-gray-700">
          {comment.author.nickname} · {comment.author.generation}기 · {partLabel(comment.author.part)}
        </span>
        <span>{formatDateTime(comment.createdAt)}</span>
      </p>

      {error && <ErrorMessage message={error} />}

      {isEditing ? (
        <form onSubmit={handleUpdate} noValidate className="mt-1 space-y-2">
          <label htmlFor={editId} className="sr-only">
            댓글 수정
          </label>
          <textarea
            id={editId}
            rows={3}
            value={editContent}
            maxLength={CONTENT_MAX}
            onChange={(e) => setEditContent(e.target.value)}
            className={textareaClass}
          />
          <div className="flex gap-2">
            <button
              type="submit"
              disabled={isWorking}
              className="rounded-md bg-gray-900 px-3 py-1 text-sm text-white hover:bg-gray-700 disabled:opacity-50"
            >
              저장
            </button>
            <button
              type="button"
              onClick={() => setIsEditing(false)}
              disabled={isWorking}
              className="rounded-md border border-gray-300 px-3 py-1 text-sm hover:bg-gray-50"
            >
              취소
            </button>
          </div>
        </form>
      ) : (
        <p className="whitespace-pre-wrap break-words text-sm text-gray-800">{comment.content}</p>
      )}

      {comment.isAuthor && !isEditing && (
        <div className="mt-2 flex flex-wrap items-center gap-2">
          {isConfirmingDelete ? (
            <>
              <span className="text-xs text-gray-600">정말 삭제할까요?</span>
              <button
                type="button"
                onClick={handleDelete}
                disabled={isWorking}
                className="rounded-md bg-red-600 px-2 py-0.5 text-xs text-white hover:bg-red-500 disabled:opacity-50"
              >
                삭제 확인
              </button>
              <button
                type="button"
                onClick={() => setIsConfirmingDelete(false)}
                disabled={isWorking}
                className="rounded-md border border-gray-300 px-2 py-0.5 text-xs hover:bg-gray-50"
              >
                취소
              </button>
            </>
          ) : (
            <>
              <button
                type="button"
                onClick={startEdit}
                className="rounded-md border border-gray-300 px-2 py-0.5 text-xs hover:bg-gray-50"
              >
                수정
              </button>
              <button
                type="button"
                onClick={() => setIsConfirmingDelete(true)}
                className="rounded-md border border-red-300 px-2 py-0.5 text-xs text-red-600 hover:bg-red-50"
              >
                삭제
              </button>
            </>
          )}
        </div>
      )}
    </li>
  )
}
