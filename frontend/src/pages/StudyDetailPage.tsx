import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'

import { getErrorMessage } from '../api/error'
import { deleteStudyPost, getStudyPost, updateStudyPost } from '../api/studyPosts'
import { partLabel, statusLabel, type StudyPostDetail } from '../api/types'
import { CommentSection } from '../components/CommentSection'
import { ErrorMessage, Loading } from '../components/StatusMessage'
import { formatDateTime } from '../utils/format'

export function StudyDetailPage() {
  const { id } = useParams()
  const postId = Number(id)
  const navigate = useNavigate()

  const [post, setPost] = useState<StudyPostDetail | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [isWorking, setIsWorking] = useState(false)
  // 삭제는 한 번 더 확인받아요. 브라우저 confirm 창 대신 화면 안에서 물어봐요.
  const [isConfirmingDelete, setIsConfirmingDelete] = useState(false)

  const loadPost = useCallback(async () => {
    try {
      setPost(await getStudyPost(postId))
      setLoadError(null)
    } catch (err) {
      setLoadError(getErrorMessage(err))
    }
  }, [postId])

  useEffect(() => {
    let cancelled = false
    getStudyPost(postId)
      .then((data) => {
        if (!cancelled) {
          setPost(data)
          setLoadError(null)
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) setLoadError(getErrorMessage(err))
      })
    return () => {
      cancelled = true
    }
  }, [postId])

  // 모집 마감 ↔ 모집 재개
  const handleToggleStatus = async () => {
    if (!post) return
    setActionError(null)
    setIsWorking(true)
    try {
      await updateStudyPost(post.postId, {
        studyStatus: post.studyStatus === 'RECRUITING' ? 'CLOSED' : 'RECRUITING',
      })
      await loadPost()
    } catch (err) {
      setActionError(getErrorMessage(err))
    } finally {
      setIsWorking(false)
    }
  }

  const handleDelete = async () => {
    if (!post) return
    setActionError(null)
    setIsWorking(true)
    try {
      await deleteStudyPost(post.postId)
      navigate('/studies', { replace: true })
    } catch (err) {
      setActionError(getErrorMessage(err))
      setIsWorking(false)
    }
  }

  if (loadError) {
    return (
      <section className="space-y-4">
        <ErrorMessage message={loadError} />
        <Link to="/studies" className="text-sm text-gray-700 underline">
          목록으로
        </Link>
      </section>
    )
  }
  if (!post) {
    return <Loading />
  }

  return (
    <article className="space-y-6">
      <div className="rounded-lg border border-gray-200 bg-white p-5">
        <div className="mb-2 flex items-center gap-2">
          <span
            className={
              post.studyStatus === 'RECRUITING'
                ? 'rounded bg-green-100 px-2 py-0.5 text-xs font-medium text-green-800'
                : 'rounded bg-gray-200 px-2 py-0.5 text-xs font-medium text-gray-700'
            }
          >
            {statusLabel(post.studyStatus)}
          </span>
        </div>
        <h1 className="mb-2 break-words text-2xl font-bold">{post.title}</h1>
        <p className="mb-4 flex flex-wrap gap-x-3 gap-y-1 text-xs text-gray-500">
          <span>
            {post.author.nickname} · {post.author.generation}기 · {partLabel(post.author.part)}
          </span>
          <span>작성 {formatDateTime(post.createdAt)}</span>
          {post.updatedAt !== post.createdAt && <span>수정 {formatDateTime(post.updatedAt)}</span>}
        </p>
        <p className="whitespace-pre-wrap break-words text-sm leading-relaxed text-gray-800">
          {post.content}
        </p>

        {/* 작성자에게만 보여요. 서버도 작성자가 아니면 403으로 막아요. */}
        {post.isAuthor && (
          <div className="mt-6 space-y-3 border-t border-gray-100 pt-4">
            {actionError && <ErrorMessage message={actionError} />}

            {isConfirmingDelete ? (
              <div className="flex flex-wrap items-center gap-2">
                <span className="text-sm text-gray-700">정말 삭제할까요? 댓글도 함께 삭제돼요.</span>
                <button
                  type="button"
                  onClick={handleDelete}
                  disabled={isWorking}
                  className="rounded-md bg-red-600 px-3 py-1 text-sm text-white hover:bg-red-500 disabled:opacity-50"
                >
                  삭제 확인
                </button>
                <button
                  type="button"
                  onClick={() => setIsConfirmingDelete(false)}
                  disabled={isWorking}
                  className="rounded-md border border-gray-300 px-3 py-1 text-sm hover:bg-gray-50"
                >
                  취소
                </button>
              </div>
            ) : (
              <div className="flex flex-wrap gap-2">
                <Link
                  to={`/studies/${post.postId}/edit`}
                  className="rounded-md border border-gray-300 px-3 py-1 text-sm hover:bg-gray-50"
                >
                  수정
                </Link>
                <button
                  type="button"
                  onClick={handleToggleStatus}
                  disabled={isWorking}
                  className="rounded-md border border-gray-300 px-3 py-1 text-sm hover:bg-gray-50 disabled:opacity-50"
                >
                  {post.studyStatus === 'RECRUITING' ? '모집 마감' : '모집 재개'}
                </button>
                <button
                  type="button"
                  onClick={() => setIsConfirmingDelete(true)}
                  disabled={isWorking}
                  className="rounded-md border border-red-300 px-3 py-1 text-sm text-red-600 hover:bg-red-50 disabled:opacity-50"
                >
                  삭제
                </button>
              </div>
            )}
          </div>
        )}
      </div>

      <CommentSection postId={post.postId} isClosed={post.studyStatus === 'CLOSED'} />

      <Link to="/studies" className="inline-block text-sm text-gray-700 underline">
        목록으로
      </Link>
    </article>
  )
}
