import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getErrorMessage } from '../api/client'
import { deleteStudyPost, formatDateTime, getStudyPost, updateStudyPost } from '../api/studyPosts'
import type { StudyPostDetail } from '../api/studyPosts'
import { useAuth } from '../auth/AuthContext'
import { ErrorMessage } from '../components/ErrorMessage'
import { StatusBadge } from '../components/StatusBadge'
import { cardClass } from '../components/styles'

const actionClass =
  'rounded-md border px-3 py-1.5 text-sm font-medium disabled:cursor-not-allowed disabled:opacity-50'

interface DetailResult {
  key: string
  post: StudyPostDetail | null
  error: string | null
}

export function StudyDetailPage() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  // 로그인 사용자가 바뀌면 isAuthor가 달라지므로 다시 조회한다
  const [reloadCount, setReloadCount] = useState(0)
  const requestKey = `${id}:${user?.userId ?? 'guest'}:${reloadCount}`

  const [result, setResult] = useState<DetailResult | null>(null)
  const [action, setAction] = useState<'close' | 'delete' | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  useEffect(() => {
    let ignore = false
    getStudyPost(id)
      .then((post) => {
        if (!ignore) setResult({ key: requestKey, post, error: null })
      })
      .catch((err) => {
        if (!ignore) setResult({ key: requestKey, post: null, error: getErrorMessage(err) })
      })
    return () => {
      ignore = true
    }
  }, [id, requestKey])

  // 다시 조회하는 동안에는 이전 글을 그대로 보여준다 (마감 후 깜빡임 방지)
  const loading = result?.key !== requestKey
  const post = result?.key.startsWith(`${id}:`) ? result.post : null
  const loadError = !loading ? (result?.error ?? null) : null

  const handleClose = async () => {
    setAction('close')
    setActionError(null)
    try {
      await updateStudyPost(id, { studyStatus: 'CLOSED' })
      setReloadCount((count) => count + 1)
    } catch (err) {
      setActionError(getErrorMessage(err))
    } finally {
      setAction(null)
    }
  }

  const handleDelete = async () => {
    if (!window.confirm('이 모집글을 삭제할까요? 달린 댓글도 함께 삭제됩니다.')) return
    setAction('delete')
    setActionError(null)
    try {
      await deleteStudyPost(id)
      navigate('/studies', { replace: true })
    } catch (err) {
      setActionError(getErrorMessage(err))
      setAction(null)
    }
  }

  if (!post) {
    return (
      <section className={cardClass}>
        {loading && <p className="text-center text-gray-500">불러오는 중...</p>}
        <ErrorMessage message={loadError} />
        {loadError && (
          <Link to="/studies" className="mt-4 inline-block text-sm font-medium text-indigo-600 hover:underline">
            목록으로
          </Link>
        )}
      </section>
    )
  }

  return (
    <div className="flex flex-col gap-4">
      <Link to="/studies" className="self-start text-sm font-medium text-indigo-600 hover:underline">
        ← 목록으로
      </Link>

      <article className={cardClass}>
        <div className="flex items-start gap-2">
          <StatusBadge status={post.studyStatus} />
          <h1 className="min-w-0 flex-1 break-words text-xl font-bold text-gray-900 sm:text-2xl">
            {post.title}
          </h1>
        </div>
        <div className="mt-3 flex flex-wrap gap-x-3 gap-y-1 text-sm text-gray-500">
          <span>
            {post.author.nickname} · {post.author.generation}기 · {post.author.part}
          </span>
          <span>작성 {formatDateTime(post.createdAt)}</span>
          {post.updatedAt !== post.createdAt && <span>수정 {formatDateTime(post.updatedAt)}</span>}
        </div>

        <p className="mt-6 whitespace-pre-wrap break-words text-gray-800">{post.content}</p>

        {post.isAuthor && (
          <div className="mt-6 flex flex-col gap-3 border-t border-gray-100 pt-4">
            <ErrorMessage message={actionError} />
            <div className="flex flex-wrap gap-2">
              <Link to={`/studies/${post.postId}/edit`}
                className={`${actionClass} border-gray-300 text-gray-700 hover:bg-gray-50`}>
                수정
              </Link>
              {post.studyStatus === 'RECRUITING' && (
                <button type="button" onClick={handleClose} disabled={action !== null}
                  className={`${actionClass} border-indigo-300 text-indigo-700 hover:bg-indigo-50`}>
                  {action === 'close' ? '마감 중...' : '모집 마감'}
                </button>
              )}
              <button type="button" onClick={handleDelete} disabled={action !== null}
                className={`${actionClass} border-red-300 text-red-700 hover:bg-red-50`}>
                {action === 'delete' ? '삭제 중...' : '삭제'}
              </button>
            </div>
          </div>
        )}
      </article>

      {/* 댓글(스터디 신청)은 다음 단계에서 구현 */}
      <section aria-labelledby="comments-title" className={cardClass}>
        <h2 id="comments-title" className="text-lg font-semibold text-gray-900">
          댓글
        </h2>
        <p className="mt-2 text-sm text-gray-500">댓글 기능은 준비 중입니다.</p>
      </section>
    </div>
  )
}
