import { Link, useNavigate, useParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getErrorMessage } from '../api/errors'
import { deleteStudyPost, getStudyPost, studyPostKeys, updateStudyPost } from '../api/studyPosts'
import type { StudyStatus } from '../api/types'
import { StatusBadge } from '../components/StatusBadge'
import {
  AuthorInfo,
  dangerButtonClass,
  ErrorMessage,
  Loading,
  secondaryButtonClass,
} from '../components/ui'
import { formatDateTime } from '../utils'

export function StudyDetailPage() {
  const postId = Number(useParams().postId)
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const { data: post, isPending, isError, error } = useQuery({
    queryKey: studyPostKeys.detail(postId),
    queryFn: () => getStudyPost(postId),
    enabled: Number.isInteger(postId) && postId > 0,
  })

  const statusMutation = useMutation({
    mutationFn: (studyStatus: StudyStatus) => updateStudyPost(postId, { studyStatus }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: studyPostKeys.all }),
  })

  const deleteMutation = useMutation({
    mutationFn: () => deleteStudyPost(postId),
    onSuccess: async () => {
      queryClient.removeQueries({ queryKey: studyPostKeys.detail(postId) })
      await queryClient.invalidateQueries({ queryKey: studyPostKeys.all })
      navigate('/studies', { replace: true, state: { message: '모집글을 삭제했어요.' } })
    },
  })

  if (!Number.isInteger(postId) || postId <= 0) {
    return <ErrorMessage>존재하지 않는 스터디 모집글이에요.</ErrorMessage>
  }
  if (isPending) return <Loading />
  if (isError) {
    return (
      <div className="space-y-4">
        <ErrorMessage>{getErrorMessage(error)}</ErrorMessage>
        <Link to="/studies" className="text-sm font-medium text-amber-700 underline">
          목록으로
        </Link>
      </div>
    )
  }

  const handleDelete = () => {
    if (window.confirm('모집글을 삭제할까요? 달린 신청 댓글도 함께 삭제돼요.')) {
      deleteMutation.mutate()
    }
  }

  const actionError = statusMutation.error ?? deleteMutation.error
  const nextStatus: StudyStatus = post.studyStatus === 'RECRUITING' ? 'CLOSED' : 'RECRUITING'

  return (
    <div className="space-y-6">
      <Link to="/studies" className="text-sm text-stone-500 hover:text-stone-800">
        ← 목록으로
      </Link>

      <article className="rounded-xl bg-white p-5 shadow-sm sm:p-6">
        <header className="mb-4 space-y-3 border-b border-stone-100 pb-4">
          <div className="flex items-start gap-2">
            <StatusBadge status={post.studyStatus} />
            <h1 className="min-w-0 break-words text-xl font-bold sm:text-2xl">{post.title}</h1>
          </div>
          <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
            <AuthorInfo author={post.author} />
            <span className="text-xs text-stone-400">
              작성 {formatDateTime(post.createdAt)}
              {post.updatedAt !== post.createdAt && ` · 수정 ${formatDateTime(post.updatedAt)}`}
            </span>
          </div>
        </header>

        <p className="whitespace-pre-wrap break-words leading-relaxed text-stone-800">{post.content}</p>

        {post.isAuthor && (
          <div className="mt-6 space-y-3 border-t border-stone-100 pt-4">
            {actionError && <ErrorMessage>{getErrorMessage(actionError)}</ErrorMessage>}
            <div className="flex flex-wrap gap-2">
              <Link to={`/studies/${post.postId}/edit`} className={secondaryButtonClass}>
                수정
              </Link>
              <button
                type="button"
                className={secondaryButtonClass}
                disabled={statusMutation.isPending}
                onClick={() => statusMutation.mutate(nextStatus)}
              >
                {nextStatus === 'CLOSED' ? '모집 마감' : '모집 다시 열기'}
              </button>
              <button
                type="button"
                className={dangerButtonClass}
                disabled={deleteMutation.isPending}
                onClick={handleDelete}
              >
                삭제
              </button>
            </div>
          </div>
        )}
      </article>
    </div>
  )
}
