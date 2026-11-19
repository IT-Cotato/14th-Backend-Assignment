import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { commentKeys, createComment, deleteComment, getComments, updateComment } from '../api/comments'
import { getErrorMessage } from '../api/errors'
import { studyPostKeys } from '../api/studyPosts'
import type { StudyPostComment, StudyStatus } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { formatDateTime } from '../utils'
import { Pagination } from './Pagination'
import {
  AuthorInfo,
  dangerButtonClass,
  ErrorMessage,
  inputClass,
  Loading,
  primaryButtonClass,
  secondaryButtonClass,
} from './ui'

const CONTENT_MAX = 500
const PAGE_SIZE = 20

function validateContent(content: string) {
  if (!content.trim()) return '댓글 내용을 입력해 주세요.'
  if (content.length > CONTENT_MAX) return `댓글은 ${CONTENT_MAX}자 이하로 입력해 주세요.`
  return ''
}

/** 모집글의 댓글 = 스터디 신청 */
export function CommentSection({ postId, studyStatus }: { postId: number; studyStatus: StudyStatus }) {
  const { isLoggedIn } = useAuth()
  const [page, setPage] = useState(0)

  const { data, isPending, isError, error } = useQuery({
    queryKey: commentKeys.list(postId, page),
    queryFn: () => getComments(postId, page, PAGE_SIZE),
    placeholderData: keepPreviousData,
  })

  return (
    <section aria-labelledby="comments-heading" className="rounded-xl bg-white p-5 shadow-sm sm:p-6">
      <h2 id="comments-heading" className="mb-4 text-lg font-bold">
        신청 댓글 {data ? data.totalElements : ''}
      </h2>

      {isLoggedIn ? (
        studyStatus === 'CLOSED' ? (
          <p className="mb-4 rounded-lg bg-stone-100 px-4 py-3 text-sm text-stone-600">
            모집이 마감되어 더 이상 신청할 수 없어요.
          </p>
        ) : (
          <CommentForm postId={postId} onCreated={() => setPage(0)} />
        )
      ) : (
        <p className="mb-4 rounded-lg bg-stone-100 px-4 py-3 text-sm text-stone-600">
          <Link to="/login" state={{ from: `/studies/${postId}` }} className="font-medium text-amber-700 underline">
            로그인
          </Link>
          하면 댓글로 스터디를 신청할 수 있어요.
        </p>
      )}

      {isPending ? (
        <Loading label="댓글을 불러오는 중이에요..." />
      ) : isError ? (
        <ErrorMessage>{getErrorMessage(error)}</ErrorMessage>
      ) : data.content.length === 0 ? (
        <p className="py-6 text-center text-sm text-stone-500">아직 신청 댓글이 없어요.</p>
      ) : (
        <>
          <ul aria-label="신청 댓글 목록" className="divide-y divide-stone-100">
            {data.content.map((comment) => (
              <CommentItem key={comment.commentId} postId={postId} comment={comment} />
            ))}
          </ul>
          <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} label="댓글 페이지" />
        </>
      )}
    </section>
  )
}

function useInvalidateComments(postId: number) {
  const queryClient = useQueryClient()
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: commentKeys.byPost(postId) }),
      // 목록 화면의 댓글 수
      queryClient.invalidateQueries({ queryKey: studyPostKeys.all }),
    ])
}

function CommentForm({ postId, onCreated }: { postId: number; onCreated: () => void }) {
  const [content, setContent] = useState('')
  const [fieldError, setFieldError] = useState('')
  const invalidate = useInvalidateComments(postId)

  const mutation = useMutation({
    mutationFn: () => createComment(postId, content),
    onSuccess: async () => {
      setContent('')
      onCreated()
      await invalidate()
    },
  })

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    const message = validateContent(content)
    setFieldError(message)
    if (!message) mutation.mutate()
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="mb-5 space-y-2">
      {mutation.isError && <ErrorMessage>{getErrorMessage(mutation.error)}</ErrorMessage>}
      <label htmlFor="new-comment" className="block text-sm font-medium text-stone-700">
        신청 댓글
      </label>
      <textarea
        id="new-comment"
        rows={3}
        maxLength={CONTENT_MAX}
        placeholder="간단한 자기소개와 참여하고 싶은 이유를 남겨 주세요."
        className={`${inputClass} resize-y`}
        aria-invalid={fieldError ? true : undefined}
        aria-describedby={fieldError ? 'new-comment-error' : undefined}
        value={content}
        onChange={(e) => {
          setContent(e.target.value)
          setFieldError('')
        }}
      />
      <div className="flex flex-wrap items-center justify-between gap-2">
        {fieldError ? (
          <p id="new-comment-error" className="text-xs text-red-600">
            {fieldError}
          </p>
        ) : (
          <span className="text-xs text-stone-500">
            {content.length}/{CONTENT_MAX}
          </span>
        )}
        <button type="submit" disabled={mutation.isPending} className={primaryButtonClass}>
          {mutation.isPending ? '신청하는 중...' : '신청하기'}
        </button>
      </div>
    </form>
  )
}

function CommentItem({ postId, comment }: { postId: number; comment: StudyPostComment }) {
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState(comment.content)
  const [fieldError, setFieldError] = useState('')
  const invalidate = useInvalidateComments(postId)
  const editId = `edit-comment-${comment.commentId}`

  const updateMutation = useMutation({
    mutationFn: () => updateComment(comment.commentId, draft),
    onSuccess: async () => {
      setEditing(false)
      await invalidate()
    },
  })

  const deleteMutation = useMutation({
    mutationFn: () => deleteComment(comment.commentId),
    onSuccess: invalidate,
  })

  const handleSave = (event: FormEvent) => {
    event.preventDefault()
    const message = validateContent(draft)
    setFieldError(message)
    if (!message) updateMutation.mutate()
  }

  const handleDelete = () => {
    if (window.confirm('신청 댓글을 삭제할까요?')) deleteMutation.mutate()
  }

  const actionError = updateMutation.error ?? deleteMutation.error

  return (
    <li className="py-4">
      <div className="mb-1 flex flex-wrap items-center gap-x-3 gap-y-1">
        <AuthorInfo author={comment.author} />
        <span className="text-xs text-stone-400">{formatDateTime(comment.createdAt)}</span>
      </div>

      {actionError && (
        <div className="my-2">
          <ErrorMessage>{getErrorMessage(actionError)}</ErrorMessage>
        </div>
      )}

      {editing ? (
        <form onSubmit={handleSave} noValidate className="mt-2 space-y-2">
          <label htmlFor={editId} className="sr-only">
            댓글 수정
          </label>
          <textarea
            id={editId}
            rows={3}
            maxLength={CONTENT_MAX}
            className={`${inputClass} resize-y`}
            aria-invalid={fieldError ? true : undefined}
            value={draft}
            onChange={(e) => {
              setDraft(e.target.value)
              setFieldError('')
            }}
          />
          {fieldError && <p className="text-xs text-red-600">{fieldError}</p>}
          <div className="flex justify-end gap-2">
            <button
              type="button"
              className={secondaryButtonClass}
              onClick={() => {
                setEditing(false)
                setDraft(comment.content)
                setFieldError('')
              }}
            >
              취소
            </button>
            <button type="submit" disabled={updateMutation.isPending} className={primaryButtonClass}>
              저장
            </button>
          </div>
        </form>
      ) : (
        <p className="whitespace-pre-wrap break-words text-stone-800">{comment.content}</p>
      )}

      {comment.isAuthor && !editing && (
        <div className="mt-2 flex gap-2">
          <button
            type="button"
            className={`${secondaryButtonClass} px-3 py-1 text-xs`}
            onClick={() => {
              setDraft(comment.content)
              setEditing(true)
            }}
          >
            댓글 수정
          </button>
          <button
            type="button"
            className={`${dangerButtonClass} px-3 py-1 text-xs`}
            disabled={deleteMutation.isPending}
            onClick={handleDelete}
          >
            댓글 삭제
          </button>
        </div>
      )}
    </li>
  )
}
