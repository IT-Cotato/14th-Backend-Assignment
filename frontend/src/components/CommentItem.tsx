import { useState, type FormEvent } from 'react'
import { deleteComment, updateComment } from '../api/comments'
import { getErrorMessage } from '../api/errors'
import type { Comment } from '../api/types'
import { ErrorAlert } from './ErrorAlert'
import {
  dangerButtonClassName,
  inputClassName,
  secondaryButtonClassName,
  smallPrimaryButtonClassName,
} from './form'
import { formatAuthor, formatDateTime } from './format'
import { COMMENT_MAX, validateComment } from './commentValidation'

interface CommentItemProps {
  comment: Comment
  // 수정하거나 삭제한 뒤 목록을 다시 불러옵니다.
  onChanged: () => void
}

// 댓글 하나입니다. 내 댓글(isAuthor)에만 수정, 삭제 버튼을 보여줍니다.
export function CommentItem({ comment, onChanged }: CommentItemProps) {
  const [mode, setMode] = useState<'view' | 'edit' | 'confirmDelete'>('view')
  const [draft, setDraft] = useState(comment.content)
  const [error, setError] = useState<string | null>(null)
  const [isWorking, setIsWorking] = useState(false)
  const editId = `comment-edit-${comment.commentId}`

  const startEdit = () => {
    setDraft(comment.content)
    setError(null)
    setMode('edit')
  }

  const cancel = () => {
    setError(null)
    setMode('view')
  }

  const handleUpdate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const validationError = validateComment(draft)
    if (validationError) {
      setError(validationError)
      return
    }
    setError(null)
    setIsWorking(true)
    try {
      await updateComment(comment.commentId, { content: draft })
      setMode('view')
      onChanged()
    } catch (caught) {
      setError(getErrorMessage(caught))
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
    } catch (caught) {
      setError(getErrorMessage(caught))
      setIsWorking(false)
    }
  }

  return (
    <li className="flex flex-col gap-2 rounded-lg border border-gray-100 bg-white p-4">
      <p className="flex flex-wrap gap-x-3 gap-y-1 text-sm text-gray-600">
        <span className="font-medium text-gray-800">{formatAuthor(comment.author)}</span>
        <span>{formatDateTime(comment.createdAt)}</span>
      </p>

      {mode === 'edit' ? (
        <form onSubmit={handleUpdate} noValidate className="flex flex-col gap-2">
          <label htmlFor={editId} className="text-sm font-medium text-gray-800">
            댓글 수정
          </label>
          <textarea
            id={editId}
            rows={3}
            maxLength={COMMENT_MAX}
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            className={inputClassName}
          />
          <div className="flex flex-wrap gap-2">
            <button type="submit" disabled={isWorking} className={smallPrimaryButtonClassName}>
              {isWorking ? '저장 중…' : '저장'}
            </button>
            <button type="button" onClick={cancel} disabled={isWorking} className={secondaryButtonClassName}>
              취소
            </button>
          </div>
        </form>
      ) : (
        <p className="whitespace-pre-wrap break-words text-gray-900">{comment.content}</p>
      )}

      {comment.isAuthor && mode === 'view' && (
        <div className="flex flex-wrap gap-2">
          <button type="button" onClick={startEdit} className={secondaryButtonClassName}>
            수정
          </button>
          <button type="button" onClick={() => setMode('confirmDelete')} className={dangerButtonClassName}>
            삭제
          </button>
        </div>
      )}

      {mode === 'confirmDelete' && (
        <div role="group" aria-label="댓글 삭제 확인" className="flex flex-wrap items-center gap-2 rounded-md bg-red-50 p-3">
          <p className="text-sm text-red-800">이 댓글을 삭제할까요?</p>
          <button type="button" onClick={handleDelete} disabled={isWorking} className={dangerButtonClassName}>
            {isWorking ? '삭제 중…' : '삭제 확인'}
          </button>
          <button type="button" onClick={cancel} disabled={isWorking} className={secondaryButtonClassName}>
            취소
          </button>
        </div>
      )}

      <ErrorAlert message={error} />
    </li>
  )
}
