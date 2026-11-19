import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { getErrorMessage } from '../api/client'
import { createComment, deleteComment, getComments, updateComment } from '../api/comments'
import type { Comment, PageResponse } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { AuthorBadge, dangerButton, ErrorMessage, formatDate, inputClass, Loading, primaryButton, secondaryButton } from './ui'

const PAGE_SIZE = 20

export default function CommentSection({ postId, closed }: { postId: number; closed: boolean }) {
  const { isLoggedIn } = useAuth()
  const [data, setData] = useState<PageResponse<Comment> | null>(null)
  const [page, setPage] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [content, setContent] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editContent, setEditContent] = useState('')

  const load = useCallback(async () => {
    try {
      setData(await getComments(postId, page, PAGE_SIZE))
    } catch (err) {
      setError(getErrorMessage(err, '댓글을 불러오지 못했어요.'))
    }
  }, [postId, page])

  useEffect(() => {
    void load()
  }, [load])

  const handleCreate = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    if (!content.trim() || content.length > 500) return setError('댓글은 1~500자여야 해요.')
    setSubmitting(true)
    try {
      await createComment(postId, content)
      setContent('')
      await load()
    } catch (err) {
      setError(getErrorMessage(err, '댓글을 작성하지 못했어요.'))
    } finally {
      setSubmitting(false)
    }
  }

  const handleUpdate = async (commentId: number) => {
    setError(null)
    if (!editContent.trim() || editContent.length > 500) return setError('댓글은 1~500자여야 해요.')
    try {
      await updateComment(commentId, editContent)
      setEditingId(null)
      await load()
    } catch (err) {
      setError(getErrorMessage(err, '댓글을 수정하지 못했어요.'))
    }
  }

  const handleDelete = async (commentId: number) => {
    if (!window.confirm('댓글을 삭제할까요?')) return
    setError(null)
    try {
      await deleteComment(commentId)
      await load()
    } catch (err) {
      setError(getErrorMessage(err, '댓글을 삭제하지 못했어요.'))
    }
  }

  return (
    <section aria-label="신청 댓글" className="space-y-4">
      <h2 className="text-lg font-semibold">신청 댓글 {data ? `(${data.totalElements})` : ''}</h2>

      {isLoggedIn ? (
        closed ? (
          <p className="rounded-md bg-gray-100 px-3 py-2 text-sm text-gray-600">모집이 마감되어 댓글을 쓸 수 없어요.</p>
        ) : (
          <form onSubmit={handleCreate} noValidate className="space-y-2">
            <label htmlFor="comment" className="block text-sm font-medium text-gray-700">신청 댓글 작성</label>
            <textarea id="comment" rows={3} maxLength={500} className={inputClass} value={content} onChange={(e) => setContent(e.target.value)} />
            <div className="flex justify-end">
              <button type="submit" className={primaryButton} disabled={submitting}>
                {submitting ? '등록 중...' : '신청하기'}
              </button>
            </div>
          </form>
        )
      ) : (
        <p className="text-sm text-gray-600">
          <Link to="/login" className="text-indigo-600 hover:underline">로그인</Link>하면 댓글로 신청할 수 있어요.
        </p>
      )}

      <ErrorMessage message={error} />

      {!data ? (
        <Loading />
      ) : data.content.length === 0 ? (
        <p className="text-sm text-gray-500">아직 신청 댓글이 없어요.</p>
      ) : (
        <ul className="divide-y divide-gray-100 rounded-lg border border-gray-200 bg-white">
          {data.content.map((c) => (
            <li key={c.commentId} className="space-y-2 p-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <AuthorBadge author={c.author} />
                <span className="text-xs text-gray-400">{formatDate(c.createdAt)}</span>
              </div>
              {editingId === c.commentId ? (
                <div className="space-y-2">
                  <label htmlFor={`edit-${c.commentId}`} className="sr-only">수정할 댓글</label>
                  <textarea id={`edit-${c.commentId}`} rows={3} maxLength={500} className={inputClass} value={editContent} onChange={(e) => setEditContent(e.target.value)} />
                  <div className="flex justify-end gap-2">
                    <button type="button" className={secondaryButton} onClick={() => setEditingId(null)}>취소</button>
                    <button type="button" className={primaryButton} onClick={() => handleUpdate(c.commentId)}>저장</button>
                  </div>
                </div>
              ) : (
                <p className="whitespace-pre-wrap break-words text-sm text-gray-800">{c.content}</p>
              )}
              {c.isAuthor && editingId !== c.commentId && (
                <div className="flex justify-end gap-2">
                  <button type="button" className={secondaryButton} onClick={() => { setEditingId(c.commentId); setEditContent(c.content) }}>
                    댓글 수정
                  </button>
                  <button type="button" className={dangerButton} onClick={() => handleDelete(c.commentId)}>
                    댓글 삭제
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}

      {data && data.totalPages > 1 && (
        <nav aria-label="댓글 페이지 이동" className="flex items-center justify-center gap-3">
          <button type="button" className={secondaryButton} disabled={page <= 0} onClick={() => setPage(page - 1)}>이전</button>
          <span className="text-sm text-gray-600">{page + 1} / {data.totalPages}</span>
          <button type="button" className={secondaryButton} disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>다음</button>
        </nav>
      )}
    </section>
  )
}
