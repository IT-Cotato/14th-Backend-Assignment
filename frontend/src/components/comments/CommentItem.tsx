import { useState, type FormEvent } from 'react'
import { deleteComment, updateComment } from '../../api/comments'
import { getErrorMessage } from '../../api/error'
import type { Comment } from '../../api/types'
import { formatDate } from '../../utils/date'
import { AuthorInfo } from '../AuthorInfo'
import { ErrorAlert } from '../ErrorAlert'
import { TextAreaField } from '../FormField'
import { COMMENT_MAX, validateComment } from './commentRules'

interface CommentItemProps {
  comment: Comment
  onUpdated: (commentId: number, content: string) => void
  onDeleted: (commentId: number) => void
}

const smallButtonClass =
  'rounded-md border px-2.5 py-1 text-xs font-medium focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-500 disabled:cursor-not-allowed disabled:opacity-50'

export function CommentItem({ comment, onUpdated, onDeleted }: CommentItemProps) {
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState(comment.content)
  const [fieldError, setFieldError] = useState<string>()
  const [serverError, setServerError] = useState<string | null>(null)
  const [pending, setPending] = useState<'save' | 'delete' | null>(null)

  const startEdit = () => {
    setDraft(comment.content)
    setFieldError(undefined)
    setServerError(null)
    setEditing(true)
  }

  // 취소하면 고치던 내용을 버리고 원래 내용으로 돌아가요
  const cancelEdit = () => {
    setEditing(false)
    setFieldError(undefined)
    setServerError(null)
  }

  const handleSave = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    if (pending) return
    const error = validateComment(draft)
    setFieldError(error)
    if (error) return

    setServerError(null)
    setPending('save')
    try {
      const content = draft.trim()
      await updateComment(comment.commentId, content)
      onUpdated(comment.commentId, content)
      setEditing(false)
    } catch (err) {
      setServerError(getErrorMessage(err))
    } finally {
      setPending(null)
    }
  }

  const handleDelete = async () => {
    if (!window.confirm('이 댓글을 삭제할까요?')) return
    setServerError(null)
    setPending('delete')
    try {
      await deleteComment(comment.commentId)
      onDeleted(comment.commentId) // 목록에서 빠지면서 이 컴포넌트도 사라져요
    } catch (err) {
      setServerError(getErrorMessage(err))
      setPending(null)
    }
  }

  return (
    <li className="py-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
          <AuthorInfo author={comment.author} />
          <span className="text-xs text-slate-400">{formatDate(comment.createdAt)}</span>
        </div>
        {comment.isAuthor && !editing && (
          <div className="flex gap-1.5">
            {/* 화면 글자는 수정/삭제지만, 모집글의 삭제 버튼과 구분되게 접근 이름은 "댓글 수정/삭제"예요 */}
            <button
              type="button"
              aria-label="댓글 수정"
              onClick={startEdit}
              disabled={pending !== null}
              className={`${smallButtonClass} border-slate-300 bg-white text-slate-700 hover:bg-slate-50`}
            >
              수정
            </button>
            <button
              type="button"
              aria-label="댓글 삭제"
              onClick={handleDelete}
              disabled={pending !== null}
              aria-busy={pending === 'delete'}
              className={`${smallButtonClass} border-red-200 bg-white text-red-600 hover:bg-red-50`}
            >
              {pending === 'delete' ? '삭제 중...' : '삭제'}
            </button>
          </div>
        )}
      </div>

      {editing ? (
        <form noValidate onSubmit={handleSave} className="mt-3 space-y-2">
          <TextAreaField
            label="수정할 내용"
            name="content"
            minHeightClass="min-h-24"
            maxCount={COMMENT_MAX}
            value={draft}
            error={fieldError}
            onChange={(e) => {
              setDraft(e.target.value)
              setFieldError(undefined)
            }}
          />
          <ErrorAlert message={serverError} />
          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={cancelEdit}
              disabled={pending === 'save'}
              className={`${smallButtonClass} border-slate-300 bg-white px-3 py-1.5 text-sm text-slate-700 hover:bg-slate-50`}
            >
              취소
            </button>
            <button
              type="submit"
              disabled={pending === 'save'}
              aria-busy={pending === 'save'}
              className={`${smallButtonClass} border-indigo-600 bg-indigo-600 px-3 py-1.5 text-sm text-white hover:bg-indigo-700`}
            >
              {pending === 'save' ? '저장 중...' : '저장'}
            </button>
          </div>
        </form>
      ) : (
        <>
          <p className="mt-2 whitespace-pre-wrap break-words text-slate-800">{comment.content}</p>
          {serverError && (
            <div className="mt-2">
              <ErrorAlert message={serverError} />
            </div>
          )}
        </>
      )}
    </li>
  )
}
