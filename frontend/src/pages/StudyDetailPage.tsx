import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router'
import { getErrorMessage } from '../api/client'
import { deleteStudyPost, fetchStudyPost, updateStudyPost } from '../api/studyPosts'
import { CommentSection } from '../components/CommentSection'
import { ErrorMessage } from '../components/ErrorMessage'
import { Loading } from '../components/Loading'
import { StatusBadge } from '../components/StatusBadge'
import { cardClass, dangerButtonClass, secondaryButtonClass } from '../components/styles'
import { formatAuthor, formatDateTime } from '../lib/format'

export function StudyDetailPage() {
  const postId = Number(useParams().id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const { data: post, isPending, isError, error } = useQuery({
    queryKey: ['studyPost', postId],
    queryFn: () => fetchStudyPost(postId),
  })

  const closeMutation = useMutation({
    mutationFn: () => updateStudyPost(postId, { studyStatus: 'CLOSED' }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['studyPost', postId] })
      await queryClient.invalidateQueries({ queryKey: ['studyPosts'] })
    },
  })

  const deleteMutation = useMutation({
    mutationFn: () => deleteStudyPost(postId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['studyPosts'] })
      navigate('/studies')
    },
  })

  const handleDelete = () => {
    if (window.confirm('모집글을 삭제할까요? 달린 댓글도 함께 삭제됩니다.')) {
      deleteMutation.mutate()
    }
  }

  if (isPending) {
    return <Loading />
  }
  if (isError) {
    return (
      <div className="space-y-4">
        <ErrorMessage message={getErrorMessage(error)} />
        <Link to="/studies" className={secondaryButtonClass}>
          목록으로
        </Link>
      </div>
    )
  }

  const actionError = closeMutation.isError
    ? getErrorMessage(closeMutation.error)
    : deleteMutation.isError
      ? getErrorMessage(deleteMutation.error)
      : null

  return (
    <div className="space-y-6">
      <article className={cardClass}>
        <div className="flex items-start justify-between gap-3">
          <h1 className="break-all text-xl font-bold">{post.title}</h1>
          <StatusBadge status={post.studyStatus} />
        </div>
        <p className="mt-2 flex flex-wrap gap-x-3 gap-y-1 text-xs text-slate-500">
          <span>{formatAuthor(post.author)}</span>
          <span>작성 {formatDateTime(post.createdAt)}</span>
          <span>수정 {formatDateTime(post.updatedAt)}</span>
        </p>
        <p className="mt-4 whitespace-pre-wrap break-words text-sm leading-6 text-slate-800">
          {post.content}
        </p>

        {post.isAuthor && (
          <div className="mt-6 flex flex-wrap gap-2 border-t border-slate-100 pt-4">
            <Link to={`/studies/${post.postId}/edit`} className={secondaryButtonClass}>
              모집글 수정
            </Link>
            {post.studyStatus === 'RECRUITING' && (
              <button
                type="button"
                onClick={() => closeMutation.mutate()}
                disabled={closeMutation.isPending}
                className={secondaryButtonClass}
              >
                모집 마감하기
              </button>
            )}
            <button
              type="button"
              onClick={handleDelete}
              disabled={deleteMutation.isPending}
              className={dangerButtonClass}
            >
              모집글 삭제
            </button>
          </div>
        )}
        {actionError && (
          <div className="mt-4">
            <ErrorMessage message={actionError} />
          </div>
        )}
      </article>

      <CommentSection postId={post.postId} isClosed={post.studyStatus === 'CLOSED'} />

      <Link to="/studies" className={secondaryButtonClass}>
        목록으로
      </Link>
    </div>
  )
}
