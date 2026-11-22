import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router'
import { ApiError, errorMessage } from '../api/client'
import { deleteStudyPost, getStudyPost, studyStatusLabels } from '../api/studyPosts'
import StudyPostComments from '../components/StudyPostComments'
import { useReadRequest } from '../hooks/useReadRequest'

type DetailProps = {
  postId: string
  token: string | null
  listSearch: string
  onUnauthorized: (token: string) => void
}

function AuthorTools({ postId, token, listSearch, onUnauthorized }: DetailProps & { token: string }) {
  const navigate = useNavigate()
  const [confirming, setConfirming] = useState(false)
  const [pending, setPending] = useState(false)
  const [blocked, setBlocked] = useState(false)
  const [error, setError] = useState('')
  const inFlight = useRef<AbortController | null>(null)
  useEffect(() => () => inFlight.current?.abort(), [])

  async function removePost() {
    if (inFlight.current || blocked) return
    const controller = new AbortController()
    inFlight.current = controller
    setPending(true)
    setError('')
    try {
      await deleteStudyPost(postId, token, controller.signal)
      if (!controller.signal.aborted) navigate({ pathname: '/study-posts', search: listSearch }, { replace: true })
    } catch (error) {
      if (controller.signal.aborted) return
      if (error instanceof ApiError && error.status === 401) {
        onUnauthorized(token)
        return
      }
      if (error instanceof ApiError && (error.status === 403 || error.status === 404)) {
        setBlocked(true)
        setConfirming(false)
      }
      setError(errorMessage(error))
    } finally {
      inFlight.current = null
      if (!controller.signal.aborted) setPending(false)
    }
  }

  return (
    <section className="author-tools" aria-labelledby="author-tools-title">
      <h2 id="author-tools-title">내가 작성한 모집글</h2>
      {!blocked && !confirming && <div className="study-actions">
        <Link className="button-link" to={{ pathname: `/study-posts/${postId}/edit`, search: listSearch }}>수정</Link>
        <button className="danger" type="button" onClick={() => setConfirming(true)}>삭제</button>
      </div>}
      {error && <p className="error" role="alert">{error}</p>}
      {confirming && <div className="delete-confirm" role="group" aria-label="모집글 삭제 확인" aria-busy={pending}>
        <p>이 모집글을 삭제할까요? 연결된 댓글도 함께 삭제됩니다.</p>
        <div className="study-actions">
          <button className="danger" type="button" disabled={pending} onClick={removePost}>
            {pending ? '삭제 중…' : '삭제 확인'}
          </button>
          <button type="button" disabled={pending} onClick={() => { setConfirming(false); setError('') }}>취소</button>
        </div>
      </div>}
    </section>
  )
}

function DetailContent({ postId, token, listSearch, onUnauthorized }: DetailProps) {
  const load = useCallback((signal: AbortSignal) => getStudyPost(postId, token, signal), [postId, token])
  const { result, retry } = useReadRequest(load)

  if (!result) return <p role="status">모집글을 불러오는 중입니다.</p>
  if ('error' in result) {
    const notFound = result.error instanceof ApiError &&
      (result.error.status === 404 || result.error.code === 'STUDY-001')
    return <>
      <h1>{notFound ? '모집글을 찾을 수 없습니다' : '모집글 상세'}</h1>
      <p className="error" role="alert">{errorMessage(result.error)}</p>
      {!notFound && <button type="button" onClick={retry}>다시 시도</button>}
    </>
  }

  const post = result.data
  return (
    <article className="study-detail">
      <span className="post-status" data-status={post.studyStatus}>{studyStatusLabels[post.studyStatus]}</span>
      <h1>{post.title}</h1>
      <dl className="detail-meta">
        <div><dt>작성자</dt><dd>{post.author.nickname} · {post.author.generation}기 · {post.author.part}</dd></div>
        <div><dt>작성 시각</dt><dd><time dateTime={post.createdAt}>{displayDate(post.createdAt)}</time></dd></div>
        <div><dt>수정 시각</dt><dd><time dateTime={post.updatedAt}>{displayDate(post.updatedAt)}</time></dd></div>
      </dl>
      <p className="post-content">{post.content}</p>
      {post.isAuthor && token && <AuthorTools postId={String(post.postId)} token={token}
        listSearch={listSearch} onUnauthorized={onUnauthorized} />}
      <StudyPostComments postId={String(post.postId)} status={post.studyStatus} token={token}
        onUnauthorized={onUnauthorized} />
    </article>
  )
}

function displayDate(value: string) {
  return value.slice(0, 16).replace('T', ' ')
}

export default function StudyPostDetailPage({ token, onUnauthorized }: {
  token: string | null
  onUnauthorized: (token: string) => void
}) {
  const { postId } = useParams()
  const { state } = useLocation()
  const search = typeof state?.listSearch === 'string' && state.listSearch.startsWith('?') ? state.listSearch : ''
  return (
    <section className="panel study-panel">
      <Link className="back-link" to={{ pathname: '/study-posts', search }}>← 모집글 목록</Link>
      <DetailContent key={JSON.stringify([postId, token])} postId={postId ?? ''} token={token}
        listSearch={search} onUnauthorized={onUnauthorized} />
    </section>
  )
}
