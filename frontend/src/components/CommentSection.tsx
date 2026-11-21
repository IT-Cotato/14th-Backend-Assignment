import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import { getErrorMessage } from '../api/client'
import { createComment, deleteComment, fetchComments, updateComment } from '../api/comments'
import { useAuth } from '../auth/AuthContext'
import { formatAuthor, formatDateTime } from '../lib/format'
import type { Comment } from '../types'
import { ErrorMessage } from './ErrorMessage'
import { Loading } from './Loading'
import { Pagination } from './Pagination'
import {
  cardClass,
  dangerButtonClass,
  inputClass,
  labelClass,
  primaryButtonClass,
  secondaryButtonClass,
} from './styles'

const PAGE_SIZE = 20
const PAGE_PARAM = 'commentPage'

interface CommentSectionProps {
  postId: number
  /** 모집이 마감된 글이면 댓글을 새로 쓸 수 없다 */
  isClosed: boolean
}

export function CommentSection({ postId, isClosed }: CommentSectionProps) {
  const { isLoggedIn } = useAuth()
  const [searchParams] = useSearchParams()
  const pageParam = Number(searchParams.get(PAGE_PARAM))
  const page = Number.isInteger(pageParam) && pageParam > 0 ? pageParam : 0

  const { data, isPending, isError, error } = useQuery({
    queryKey: ['comments', postId, page],
    queryFn: () => fetchComments(postId, page, PAGE_SIZE),
  })

  return (
    <section aria-labelledby="comment-heading" className={cardClass}>
      <h2 id="comment-heading" className="text-lg font-bold">
        댓글 (스터디 신청){data ? ` ${data.totalElements}개` : ''}
      </h2>
      <p className="mt-1 text-xs text-slate-500">댓글을 남기면 스터디 신청이 됩니다.</p>

      <div className="mt-4 space-y-4">
        {isPending && <Loading />}
        {isError && <ErrorMessage message={getErrorMessage(error)} />}
        {data && data.content.length === 0 && (
          <p className="text-sm text-slate-500">아직 댓글이 없습니다.</p>
        )}
        {data && data.content.length > 0 && (
          <ul className="divide-y divide-slate-100">
            {data.content.map((comment) => (
              <CommentItem key={comment.commentId} postId={postId} comment={comment} />
            ))}
          </ul>
        )}
        {data && (
          <Pagination
            page={data.page}
            totalPages={data.totalPages}
            buildHref={(nextPage) => `?${PAGE_PARAM}=${nextPage}`}
          />
        )}

        {isClosed ? (
          <p className="rounded-md bg-slate-100 px-3 py-2 text-sm text-slate-600">
            모집이 마감되어 댓글을 작성할 수 없습니다.
          </p>
        ) : isLoggedIn ? (
          <CommentForm postId={postId} />
        ) : (
          <p className="rounded-md bg-slate-100 px-3 py-2 text-sm text-slate-600">
            댓글을 작성하려면{' '}
            <Link to="/login" className="font-semibold text-indigo-600 hover:underline">
              로그인
            </Link>
            이 필요합니다.
          </p>
        )}
      </div>
    </section>
  )
}

function CommentForm({ postId }: { postId: number }) {
  const queryClient = useQueryClient()
  const [content, setContent] = useState('')

  const mutation = useMutation({
    mutationFn: () => createComment(postId, content),
    onSuccess: async () => {
      setContent('')
      await queryClient.invalidateQueries({ queryKey: ['comments', postId] })
      await queryClient.invalidateQueries({ queryKey: ['studyPosts'] })
    },
  })

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    mutation.mutate()
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="space-y-2">
      <label htmlFor="comment-content" className={labelClass}>
        댓글 내용
      </label>
      <textarea
        id="comment-content"
        rows={3}
        maxLength={500}
        value={content}
        onChange={(event) => setContent(event.target.value)}
        className={inputClass}
      />
      <ErrorMessage message={mutation.isError ? getErrorMessage(mutation.error) : null} />
      <button type="submit" disabled={mutation.isPending} className={primaryButtonClass}>
        {mutation.isPending ? '작성하는 중...' : '댓글 작성'}
      </button>
    </form>
  )
}

function CommentItem({ postId, comment }: { postId: number; comment: Comment }) {
  const queryClient = useQueryClient()
  const [isEditing, setIsEditing] = useState(false)
  const [draft, setDraft] = useState(comment.content)
  const editInputId = `comment-edit-${comment.commentId}`

  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: ['comments', postId] })
    await queryClient.invalidateQueries({ queryKey: ['studyPosts'] })
  }

  const updateMutation = useMutation({
    mutationFn: () => updateComment(comment.commentId, draft),
    onSuccess: async () => {
      await refresh()
      setIsEditing(false)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: () => deleteComment(comment.commentId),
    onSuccess: refresh,
  })

  const startEditing = () => {
    setDraft(comment.content)
    updateMutation.reset()
    setIsEditing(true)
  }

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    updateMutation.mutate()
  }

  const handleDelete = () => {
    if (window.confirm('댓글을 삭제할까요?')) {
      deleteMutation.mutate()
    }
  }

  return (
    <li className="py-3">
      <p className="flex flex-wrap gap-x-3 gap-y-1 text-xs text-slate-500">
        <span className="font-semibold text-slate-700">{formatAuthor(comment.author)}</span>
        <span>{formatDateTime(comment.createdAt)}</span>
      </p>

      {isEditing ? (
        <form onSubmit={handleSubmit} noValidate className="mt-2 space-y-2">
          <label htmlFor={editInputId} className={labelClass}>
            수정할 댓글 내용
          </label>
          <textarea
            id={editInputId}
            rows={3}
            maxLength={500}
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            className={inputClass}
          />
          <ErrorMessage
            message={updateMutation.isError ? getErrorMessage(updateMutation.error) : null}
          />
          <div className="flex flex-wrap gap-2">
            <button type="submit" disabled={updateMutation.isPending} className={primaryButtonClass}>
              {updateMutation.isPending ? '저장하는 중...' : '댓글 저장'}
            </button>
            <button
              type="button"
              onClick={() => setIsEditing(false)}
              className={secondaryButtonClass}
            >
              취소
            </button>
          </div>
        </form>
      ) : (
        <>
          <p className="mt-1 whitespace-pre-wrap break-words text-sm text-slate-800">
            {comment.content}
          </p>
          {comment.isAuthor && (
            <div className="mt-2 flex flex-wrap gap-2">
              <button type="button" onClick={startEditing} className={secondaryButtonClass}>
                댓글 수정
              </button>
              <button
                type="button"
                onClick={handleDelete}
                disabled={deleteMutation.isPending}
                className={dangerButtonClass}
              >
                댓글 삭제
              </button>
            </div>
          )}
          <ErrorMessage
            message={deleteMutation.isError ? getErrorMessage(deleteMutation.error) : null}
          />
        </>
      )}
    </li>
  )
}
