import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { createComment, deleteComment, getComments, updateComment } from '../api/comments.ts'
import { getErrorMessage } from '../api/client.ts'
import { PART_LABELS, type Comment, type PageResponse } from '../api/types.ts'
import { useAuth } from '../auth/useAuth.ts'
import Pagination from './Pagination.tsx'

interface Props {
  postId: number
  // 모집이 마감된 글에는 댓글을 쓸 수 없다
  closed: boolean
}

export default function CommentSection({ postId, closed }: Props) {
  const { user } = useAuth()
  const [page, setPage] = useState(0)
  const [result, setResult] = useState<PageResponse<Comment> | null>(null)
  const [error, setError] = useState('')
  const [content, setContent] = useState('')
  const [formError, setFormError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editContent, setEditContent] = useState('')

  const load = useCallback(async (targetPage: number) => {
    try {
      setResult(await getComments(postId, targetPage))
      setError('')
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }, [postId])

  useEffect(() => {
    let cancelled = false
    getComments(postId, page)
      .then((data) => {
        if (!cancelled) {
          setResult(data)
          setError('')
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(getErrorMessage(err))
        }
      })
    return () => {
      cancelled = true
    }
  }, [postId, page])

  async function handleCreate(e: FormEvent) {
    e.preventDefault()
    setFormError('')
    if (!content.trim()) {
      setFormError('댓글 내용을 입력해 주세요.')
      return
    }
    setSubmitting(true)
    try {
      await createComment(postId, content)
      setContent('')
      // 새 댓글은 맨 뒤에 붙으므로 마지막 페이지로 이동해서 보여 준다
      const fresh = await getComments(postId, 0)
      const lastPage = Math.max(fresh.totalPages - 1, 0)
      setPage(lastPage)
      await load(lastPage)
    } catch (err) {
      setFormError(getErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleUpdate(commentId: number) {
    setError('')
    if (!editContent.trim()) {
      setError('댓글 내용을 입력해 주세요.')
      return
    }
    try {
      await updateComment(commentId, editContent)
      setEditingId(null)
      await load(page)
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  async function handleDelete(commentId: number) {
    if (!window.confirm('이 댓글을 삭제할까요?')) {
      return
    }
    setError('')
    try {
      await deleteComment(commentId)
      // 마지막 페이지의 마지막 댓글을 지웠으면 앞 페이지로 간다
      const nextPage = result && result.content.length === 1 && page > 0 ? page - 1 : page
      setPage(nextPage)
      await load(nextPage)
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  return (
    <section aria-label="댓글" className="rounded-lg bg-white p-6 shadow-sm">
      <h2 className="text-lg font-bold">댓글 {result ? result.totalElements : ''}</h2>

      {error ? <p role="alert" className="mt-2 text-sm text-red-600">{error}</p> : null}

      {result && result.content.length === 0 ? (
        <p className="mt-3 text-sm text-gray-500">아직 댓글이 없어요. 첫 신청 댓글을 남겨 보세요.</p>
      ) : null}

      <ul className="mt-3 divide-y divide-gray-100">
        {result?.content.map((comment) => (
          <li key={comment.commentId} className="py-3">
            <p className="text-sm text-gray-500">
              {comment.author.nickname} · {comment.author.generation}기 {PART_LABELS[comment.author.part]} ·{' '}
              {comment.createdAt.slice(0, 16).replace('T', ' ')}
            </p>
            {editingId === comment.commentId ? (
              <div className="mt-2 space-y-2">
                <label htmlFor={`edit-${comment.commentId}`} className="sr-only">댓글 수정</label>
                <textarea id={`edit-${comment.commentId}`} value={editContent} maxLength={500} rows={3}
                  onChange={(e) => setEditContent(e.target.value)}
                  className="w-full rounded border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none" />
                <div className="flex gap-2">
                  <button type="button" onClick={() => handleUpdate(comment.commentId)}
                    className="rounded bg-indigo-600 px-3 py-1 text-sm text-white hover:bg-indigo-700">
                    저장
                  </button>
                  <button type="button" onClick={() => setEditingId(null)}
                    className="rounded border border-gray-300 px-3 py-1 text-sm hover:bg-gray-100">
                    취소
                  </button>
                </div>
              </div>
            ) : (
              <>
                <p className="mt-1 whitespace-pre-wrap">{comment.content}</p>
                {comment.isAuthor ? (
                  <div className="mt-2 flex gap-2 text-sm">
                    <button type="button"
                      onClick={() => { setEditingId(comment.commentId); setEditContent(comment.content) }}
                      className="text-gray-600 hover:text-indigo-600">
                      수정
                    </button>
                    <button type="button" onClick={() => handleDelete(comment.commentId)}
                      className="text-red-600 hover:underline">
                      삭제
                    </button>
                  </div>
                ) : null}
              </>
            )}
          </li>
        ))}
      </ul>

      {result ? <Pagination page={result.page} totalPages={result.totalPages} onChange={setPage} /> : null}

      <div className="mt-4 border-t border-gray-100 pt-4">
        {closed ? (
          <p className="text-sm text-gray-500">모집이 마감된 글에는 댓글을 쓸 수 없어요.</p>
        ) : user ? (
          <form onSubmit={handleCreate} noValidate className="space-y-2">
            <label htmlFor="comment-content" className="block text-sm font-medium text-gray-700">
              신청 댓글
            </label>
            <textarea id="comment-content" value={content} maxLength={500} rows={3}
              onChange={(e) => setContent(e.target.value)}
              placeholder="자기소개와 참여하고 싶은 이유를 남겨 주세요."
              className="w-full rounded border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none" />
            {formError ? <p role="alert" className="text-sm text-red-600">{formError}</p> : null}
            <button type="submit" disabled={submitting}
              className="rounded bg-indigo-600 px-4 py-1.5 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50">
              {submitting ? '등록 중...' : '댓글 등록'}
            </button>
          </form>
        ) : (
          <p className="text-sm text-gray-500">
            댓글을 쓰려면 <Link to="/login" className="text-indigo-600 hover:underline">로그인</Link>이 필요해요.
          </p>
        )}
      </div>
    </section>
  )
}
