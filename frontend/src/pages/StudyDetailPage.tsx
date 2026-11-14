import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { getErrorMessage } from '../api/errors'
import { deleteStudyPost, getStudyPost, updateStudyPost } from '../api/studyPosts'
import type { StudyPostDetail } from '../api/types'
import { ErrorAlert } from '../components/ErrorAlert'
import { LoadingStatus } from '../components/LoadingStatus'
import { StatusBadge } from '../components/StatusBadge'
import { dangerButtonClassName, secondaryButtonClassName } from '../components/form'
import { formatAuthor, formatDateTime } from '../components/format'

export function StudyDetailPage() {
  const { id } = useParams()
  const postId = Number(id)
  const navigate = useNavigate()

  const [post, setPost] = useState<StudyPostDetail | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [isWorking, setIsWorking] = useState(false)
  const [isConfirmingDelete, setIsConfirmingDelete] = useState(false)
  const isLoading = post === null && loadError === null

  useEffect(() => {
    let cancelled = false
    getStudyPost(postId)
      .then((data) => {
        if (!cancelled) setPost(data)
      })
      .catch((error: unknown) => {
        if (!cancelled) setLoadError(getErrorMessage(error))
      })
    return () => {
      cancelled = true
    }
  }, [postId])

  const handleClose = async () => {
    setActionError(null)
    setIsWorking(true)
    try {
      await updateStudyPost(postId, { studyStatus: 'CLOSED' })
      setPost(await getStudyPost(postId))
    } catch (error) {
      setActionError(getErrorMessage(error))
    } finally {
      setIsWorking(false)
    }
  }

  const handleDelete = async () => {
    setActionError(null)
    setIsWorking(true)
    try {
      await deleteStudyPost(postId)
      navigate('/studies', { replace: true })
    } catch (error) {
      setActionError(getErrorMessage(error))
      setIsWorking(false)
    }
  }

  return (
    <section className="flex flex-col gap-6">
      <Link to="/studies" className="text-sm text-blue-600 hover:underline">
        ← 목록으로
      </Link>

      {isLoading && <LoadingStatus />}
      <ErrorAlert message={loadError} />

      {post && (
        <article className="flex flex-col gap-4 rounded-lg bg-white p-6 shadow-sm">
          <header className="flex flex-col gap-2">
            <div>
              <StatusBadge status={post.studyStatus} />
            </div>
            <h1 className="break-words text-2xl font-bold text-gray-900">{post.title}</h1>
            <p className="flex flex-wrap gap-x-3 gap-y-1 text-sm text-gray-600">
              <span>{formatAuthor(post.author)}</span>
              <span>{formatDateTime(post.createdAt)}</span>
            </p>
          </header>

          <p className="whitespace-pre-wrap break-words text-base text-gray-900">{post.content}</p>

          {post.isAuthor && (
            <div className="flex flex-col gap-3 border-t border-gray-100 pt-4">
              <div className="flex flex-wrap gap-2">
                <Link to={`/studies/${post.postId}/edit`} className={secondaryButtonClassName}>
                  수정
                </Link>
                {post.studyStatus === 'RECRUITING' && (
                  <button type="button" onClick={handleClose} disabled={isWorking} className={secondaryButtonClassName}>
                    모집 마감
                  </button>
                )}
                {!isConfirmingDelete && (
                  <button
                    type="button"
                    onClick={() => setIsConfirmingDelete(true)}
                    disabled={isWorking}
                    className={dangerButtonClassName}
                  >
                    삭제
                  </button>
                )}
              </div>

              {isConfirmingDelete && (
                <div
                  role="group"
                  aria-label="삭제 확인"
                  className="flex flex-wrap items-center gap-2 rounded-md bg-red-50 p-3"
                >
                  <p className="text-sm text-red-800">글과 달린 댓글이 모두 삭제돼요. 삭제할까요?</p>
                  <button type="button" onClick={handleDelete} disabled={isWorking} className={dangerButtonClassName}>
                    {isWorking ? '삭제 중…' : '삭제 확인'}
                  </button>
                  <button
                    type="button"
                    onClick={() => setIsConfirmingDelete(false)}
                    disabled={isWorking}
                    className={secondaryButtonClassName}
                  >
                    취소
                  </button>
                </div>
              )}

              <ErrorAlert message={actionError} />
            </div>
          )}
        </article>
      )}
    </section>
  )
}
