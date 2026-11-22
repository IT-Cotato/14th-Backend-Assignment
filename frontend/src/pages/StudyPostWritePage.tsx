import { useCallback, useEffect, useRef, useState, type SubmitEvent } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router'
import { ApiError, errorMessage } from '../api/client'
import {
  createStudyPost, getStudyPost, studyStatusLabels, updateStudyPost,
  type StudyPostChanges, type StudyPostDetail, type StudyStatus,
} from '../api/studyPosts'
import { useReadRequest } from '../hooks/useReadRequest'

type WriteProps = {
  token: string
  onUnauthorized: (token: string) => void
  listSearch: string
}

function StudyPostForm({ post, token, onUnauthorized, listSearch }: WriteProps & { post?: StudyPostDetail }) {
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const [blocked, setBlocked] = useState(false)
  const [pending, setPending] = useState(false)
  const inFlight = useRef<AbortController | null>(null)
  useEffect(() => () => inFlight.current?.abort(), [])
  const label = post ? '모집글 수정' : '모집글 작성'

  async function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    if (inFlight.current || blocked) return
    const form = new FormData(event.currentTarget)
    const title = String(form.get('title') ?? '')
    const content = String(form.get('content') ?? '')
    if (!title.trim() || title.length > 50 || !content.trim() || content.length > 2000) {
      setError('제목은 1~50자, 내용은 1~2000자로 입력해 주세요.')
      return
    }

    const body: StudyPostChanges = {}
    if (post) {
      const studyStatus = form.get('studyStatus') as StudyStatus
      if (title !== post.title) body.title = title
      if (content !== post.content) body.content = content
      if (studyStatus !== post.studyStatus) body.studyStatus = studyStatus
      if (!Object.keys(body).length) {
        setError('변경한 내용이 없습니다.')
        return
      }
    }

    const controller = new AbortController()
    inFlight.current = controller
    setPending(true)
    setError('')
    try {
      const { postId } = post
        ? await updateStudyPost(String(post.postId), body, token, controller.signal)
        : await createStudyPost({ title, content }, token, controller.signal)
      if (!controller.signal.aborted) {
        navigate(`/study-posts/${postId}`, { replace: true, state: { listSearch } })
      }
    } catch (error) {
      if (controller.signal.aborted) return
      if (error instanceof ApiError && error.status === 401) {
        onUnauthorized(token)
        return
      }
      if (error instanceof ApiError && (error.status === 403 || error.status === 404)) setBlocked(true)
      setError(errorMessage(error))
    } finally {
      inFlight.current = null
      if (!controller.signal.aborted) setPending(false)
    }
  }

  return (
    <>
      <h1>{label}</h1>
      {error && <p className="error" role="alert">{error}</p>}
      <form onSubmit={submit} aria-busy={pending}>
        <fieldset disabled={pending || blocked}>
          <label>제목
            <input name="title" defaultValue={post?.title} required maxLength={50} />
          </label>
          <div className="study-field">
            <label htmlFor="study-content">내용</label>
            <textarea id="study-content" name="content" rows={8} defaultValue={post?.content} required maxLength={2000} />
          </div>
          {post && <label>모집 상태
            <select name="studyStatus" defaultValue={post.studyStatus}>
              {Object.entries(studyStatusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
            </select>
          </label>}
          <button className="primary" type="submit">{pending ? '저장 중…' : '저장'}</button>
        </fieldset>
      </form>
      <Link className="back-link" to={post ? `/study-posts/${post.postId}` : { pathname: '/study-posts', search: listSearch }}
        state={post ? { listSearch } : undefined}>취소</Link>
    </>
  )
}

function EditContent({ postId, ...props }: WriteProps & { postId: string }) {
  const load = useCallback((signal: AbortSignal) => getStudyPost(postId, props.token, signal), [postId, props.token])
  const { result, retry } = useReadRequest(load)
  if (!result) return <p role="status">모집글을 불러오는 중입니다.</p>
  if ('error' in result) {
    const notFound = result.error instanceof ApiError && result.error.status === 404
    return <>
      <h1>모집글 수정</h1>
      <p role="alert" className="error">{errorMessage(result.error)}</p>
      {!notFound && <button type="button" onClick={retry}>다시 시도</button>}
    </>
  }
  if (!result.data.isAuthor) return <>
    <h1>모집글 수정</h1>
    <p role="alert" className="error">모집글 작성자만 수정할 수 있습니다.</p>
    <Link to={`/study-posts/${postId}`} state={{ listSearch: props.listSearch }}>상세로 이동</Link>
  </>
  return <StudyPostForm {...props} post={result.data} />
}

export default function StudyPostWritePage({ mode, ...props }: Omit<WriteProps, 'listSearch'> & {
  mode: 'create' | 'edit'
}) {
  const { postId } = useParams()
  const { search } = useLocation()

  return (
    <section className="panel study-panel">
      <Link className="back-link" to={{ pathname: '/study-posts', search }}>← 모집글 목록</Link>
      {mode === 'edit' ? <EditContent key={postId} {...props} postId={postId ?? ''} listSearch={search} />
        : <StudyPostForm {...props} listSearch={search} />}
    </section>
  )
}
