import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { deleteStudy, fetchStudy, updateStudy } from '../api/studies'
import type { StudyPostDetail } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { StatusBadge } from '../components/StatusBadge'
import { cardClass } from '../components/styles'
import { formatAuthor, formatDateTime } from '../utils/format'
import { getErrorMessage } from '../utils/messages'

interface LoadedPost {
  key: string
  post: StudyPostDetail | null
  error: string | null
}

export function StudyDetailPage() {
  const { id } = useParams()
  const postId = Number(id)
  const invalidId = !Number.isInteger(postId)
  const navigate = useNavigate()
  const { isAuthenticated } = useAuth()

  // 요청 조건(key)과 함께 결과를 저장해서, 로딩 여부를 렌더링 중에 계산한다.
  // 로그인 상태가 바뀌면 key가 바뀌어 isAuthor를 서버에서 다시 받아온다.
  const requestKey = JSON.stringify([postId, isAuthenticated])
  const [loaded, setLoaded] = useState<LoadedPost | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [confirmingDelete, setConfirmingDelete] = useState(false)

  useEffect(() => {
    if (invalidId) {
      return
    }
    let cancelled = false
    fetchStudy(postId)
      .then((post) => {
        if (!cancelled) setLoaded({ key: requestKey, post, error: null })
      })
      .catch((err) => {
        if (!cancelled) setLoaded({ key: requestKey, post: null, error: getErrorMessage(err) })
      })
    return () => {
      cancelled = true
    }
  }, [invalidId, postId, requestKey])

  const current = loaded?.key === requestKey ? loaded : null
  const loading = !invalidId && current === null
  const error = invalidId ? '올바르지 않은 모집글 주소예요.' : (current?.error ?? null)
  const post = current?.post ?? null

  async function handleToggleStatus() {
    if (!post || !current) return
    const next = post.studyStatus === 'RECRUITING' ? 'CLOSED' : 'RECRUITING'
    setBusy(true)
    setActionError(null)
    try {
      await updateStudy(post.postId, { studyStatus: next })
      setLoaded({ ...current, post: { ...post, studyStatus: next } })
    } catch (err) {
      setActionError(getErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  async function handleDelete() {
    if (!post) return
    setBusy(true)
    setActionError(null)
    try {
      await deleteStudy(post.postId)
      navigate('/studies', { replace: true })
    } catch (err) {
      setActionError(getErrorMessage(err))
      setConfirmingDelete(false)
      setBusy(false)
    }
  }

  if (loading) {
    return <p className="py-10 text-center text-slate-500">불러오는 중이에요...</p>
  }
  if (error || !post) {
    return (
      <div className="text-center">
        <p role="alert" className="mb-4 text-red-600">
          {error ?? '모집글을 불러오지 못했어요.'}
        </p>
        <Link to="/studies" className="text-indigo-600 hover:underline">
          스터디 목록으로 돌아가기
        </Link>
      </div>
    )
  }

  // 서버가 내려준 isAuthor를 쓰되, 로그아웃한 직후에는 버튼을 바로 숨긴다
  const canManage = post.isAuthor && isAuthenticated

  return (
    <div>
      <p className="mb-4 text-sm">
        <Link to="/studies" className="text-slate-500 hover:text-indigo-600">
          ← 목록으로
        </Link>
      </p>

      <article className={cardClass}>
        <div className="mb-2 flex items-center gap-2">
          <StatusBadge status={post.studyStatus} />
        </div>
        <h1 className="mb-2 text-2xl font-bold break-words">{post.title}</h1>
        <p className="mb-6 text-sm text-slate-500">
          {formatAuthor(post.author)} · {formatDateTime(post.createdAt)}
        </p>
        <p className="whitespace-pre-wrap break-words text-slate-800">{post.content}</p>
      </article>

      {canManage && (
        <section aria-label="모집글 관리" className="mt-4 space-y-3">
          <div className="flex flex-wrap gap-2">
            <Link
              to={`/studies/${post.postId}/edit`}
              className="rounded-md border border-slate-300 bg-white px-3 py-2 text-sm hover:bg-slate-50"
            >
              수정
            </Link>
            <button
              type="button"
              disabled={busy}
              onClick={handleToggleStatus}
              className="rounded-md border border-slate-300 bg-white px-3 py-2 text-sm hover:bg-slate-50 disabled:opacity-50"
            >
              {post.studyStatus === 'RECRUITING' ? '모집 마감' : '모집 재개'}
            </button>
            {!confirmingDelete && (
              <button
                type="button"
                disabled={busy}
                onClick={() => setConfirmingDelete(true)}
                className="rounded-md border border-red-300 bg-white px-3 py-2 text-sm text-red-600 hover:bg-red-50 disabled:opacity-50"
              >
                삭제
              </button>
            )}
          </div>

          {confirmingDelete && (
            <div role="alertdialog" aria-label="삭제 확인" className="rounded-md border border-red-200 bg-red-50 p-3">
              <p className="mb-2 text-sm text-red-700">이 모집글을 삭제할까요? 삭제하면 되돌릴 수 없어요.</p>
              <div className="flex gap-2">
                <button
                  type="button"
                  disabled={busy}
                  onClick={handleDelete}
                  className="rounded-md bg-red-600 px-3 py-2 text-sm font-semibold text-white hover:bg-red-500 disabled:opacity-50"
                >
                  삭제 확인
                </button>
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => setConfirmingDelete(false)}
                  className="rounded-md border border-slate-300 bg-white px-3 py-2 text-sm"
                >
                  취소
                </button>
              </div>
            </div>
          )}

          {actionError && (
            <p role="alert" className="text-sm text-red-600">
              {actionError}
            </p>
          )}
        </section>
      )}
    </div>
  )
}
