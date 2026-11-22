import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { getErrorMessage } from '../api/error'
import { deleteStudyPost, getStudyPost, updateStudyPost } from '../api/studyPosts'
import type { StudyPostDetail } from '../api/types'
import { AuthorInfo } from '../components/AuthorInfo'
import { ErrorAlert } from '../components/ErrorAlert'
import { Loading } from '../components/Loading'
import { StatusBadge } from '../components/StatusBadge'
import { formatDate } from '../utils/date'
import { NOT_FOUND_MESSAGE, parsePostId } from './studyPostId'

interface DetailState {
  postId: number
  post?: StudyPostDetail
  error?: string
}

const actionButtonClass =
  'rounded-md border px-3 py-2 text-sm font-medium focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-500 disabled:cursor-not-allowed disabled:opacity-50'

export function StudyDetailPage() {
  const { id } = useParams()
  const postId = parsePostId(id)
  const navigate = useNavigate()
  const location = useLocation()
  const notice = (location.state as { notice?: string } | null)?.notice

  const [state, setState] = useState<DetailState | null>(null)
  const [pendingAction, setPendingAction] = useState<'status' | 'delete' | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  useEffect(() => {
    if (postId === null) return
    let cancelled = false
    getStudyPost(postId)
      .then((post) => {
        if (!cancelled) setState({ postId, post })
      })
      .catch((error: unknown) => {
        if (!cancelled) setState({ postId, error: getErrorMessage(error) })
      })
    return () => {
      cancelled = true
    }
  }, [postId])

  if (postId === null) {
    return <NotFound message={NOT_FOUND_MESSAGE} />
  }
  if (state?.postId !== postId) {
    return <Loading />
  }
  if (!state.post) {
    return <NotFound message={state.error ?? NOT_FOUND_MESSAGE} />
  }

  const post = state.post
  const nextStatus = post.studyStatus === 'RECRUITING' ? 'CLOSED' : 'RECRUITING'

  const handleToggleStatus = async () => {
    setActionError(null)
    setPendingAction('status')
    try {
      await updateStudyPost(post.postId, { studyStatus: nextStatus })
      setState({ postId, post: { ...post, studyStatus: nextStatus } })
    } catch (error) {
      setActionError(getErrorMessage(error))
    } finally {
      setPendingAction(null)
    }
  }

  const handleDelete = async () => {
    if (!window.confirm('이 모집글을 삭제할까요? 삭제하면 되돌릴 수 없어요.')) return
    setActionError(null)
    setPendingAction('delete')
    try {
      await deleteStudyPost(post.postId)
      navigate('/studies', { replace: true })
    } catch (error) {
      setActionError(getErrorMessage(error))
      setPendingAction(null)
    }
  }

  return (
    <article className="mx-auto w-full max-w-3xl">
      {notice && (
        <p role="status" className="mb-4 rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
          {notice}
        </p>
      )}

      <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm sm:p-8">
        <div className="mb-3">
          <StatusBadge status={post.studyStatus} />
        </div>
        <h1 className="break-words text-2xl font-bold text-slate-900">{post.title}</h1>
        <div className="mt-3 flex flex-wrap items-center gap-x-3 gap-y-1 border-b border-slate-100 pb-4">
          <AuthorInfo author={post.author} />
          <span className="text-sm text-slate-400">{formatDate(post.createdAt)}</span>
        </div>

        <p className="mt-5 whitespace-pre-wrap break-words leading-relaxed text-slate-800">{post.content}</p>

        {post.isAuthor && (
          <div className="mt-8 space-y-3 border-t border-slate-100 pt-5">
            <ErrorAlert message={actionError} />
            <div className="flex flex-wrap gap-2">
              <Link
                to={`/studies/${post.postId}/edit`}
                className={`${actionButtonClass} border-slate-300 bg-white text-slate-700 hover:bg-slate-50`}
              >
                수정
              </Link>
              <button
                type="button"
                onClick={handleToggleStatus}
                disabled={pendingAction !== null}
                aria-busy={pendingAction === 'status'}
                className={`${actionButtonClass} border-indigo-200 bg-indigo-50 text-indigo-700 hover:bg-indigo-100`}
              >
                {pendingAction === 'status' ? '처리 중...' : post.studyStatus === 'RECRUITING' ? '모집 마감' : '다시 모집'}
              </button>
              <button
                type="button"
                onClick={handleDelete}
                disabled={pendingAction !== null}
                aria-busy={pendingAction === 'delete'}
                className={`${actionButtonClass} border-red-200 bg-white text-red-600 hover:bg-red-50`}
              >
                {pendingAction === 'delete' ? '삭제 중...' : '삭제'}
              </button>
            </div>
          </div>
        )}
      </div>

      {/* 다음 단계에서 댓글(= 스터디 신청) 목록과 작성 폼이 들어갈 자리예요 */}
      <section aria-labelledby="comments-heading" className="mt-6 rounded-xl border border-slate-200 bg-white p-5 shadow-sm sm:p-8">
        <h2 id="comments-heading" className="text-lg font-bold text-slate-900">
          댓글
        </h2>
        <p className="mt-2 text-sm text-slate-500">댓글 기능은 곧 추가돼요.</p>
      </section>

      <div className="mt-6">
        <Link to="/studies" className="text-sm font-medium text-indigo-600 hover:underline">
          목록으로
        </Link>
      </div>
    </article>
  )
}

function NotFound({ message }: { message: string }) {
  return (
    <div className="mx-auto w-full max-w-3xl space-y-4">
      <ErrorAlert message={message} />
      <Link to="/studies" className="text-sm font-medium text-indigo-600 hover:underline">
        목록으로
      </Link>
    </div>
  )
}
