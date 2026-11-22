import { useCallback, useEffect, useRef, useState, type SubmitEvent } from 'react'
import { Link } from 'react-router'
import { ApiError, errorMessage } from '../api/client'
import { commentPageSize, createComment, deleteComment, listComments, updateComment } from '../api/comments'
import type { StudyStatus } from '../api/studyPosts'
import { useReadRequest } from '../hooks/useReadRequest'

type CommentAction = { type: 'edit' | 'delete'; commentId: number }

type Props = {
  postId: string
  status: StudyStatus
  token: string | null
  onUnauthorized: (token: string) => void
}

export default function StudyPostComments({ postId, status, token, onUnauthorized }: Props) {
  const [page, setPage] = useState(0)
  const [draft, setDraft] = useState('')
  const [action, setAction] = useState<CommentAction | null>(null)
  const [closedByServer, setClosedByServer] = useState(false)
  const [missingPost, setMissingPost] = useState(false)
  const [denied, setDenied] = useState<number[]>([])
  const [pending, setPending] = useState<'create' | 'edit' | 'delete' | null>(null)
  const [error, setError] = useState('')
  const inFlight = useRef<AbortController | null>(null)
  useEffect(() => () => inFlight.current?.abort(), [])

  const load = useCallback((signal: AbortSignal) => listComments(postId, page, token, signal), [postId, page, token])
  const { result, retry } = useReadRequest(load)
  const data = result && 'data' in result ? result.data : null
  const listError = result && 'error' in result ? result.error : null
  const closed = status === 'CLOSED' || closedByServer
  const postUnavailable = missingPost || (listError instanceof ApiError && listError.status === 404)

  function moveTo(nextPage: number) {
    setAction(null)
    setPage(nextPage)
    retry()
  }

  async function mutate(target: CommentAction | null, content = '') {
    if (!token || inFlight.current || postUnavailable || (!target && closed)) return
    if (target?.type !== 'delete' && (!content.trim() || content.length > 500)) {
      setError('댓글 내용을 1~500자로 입력해 주세요.')
      return
    }

    const controller = new AbortController()
    inFlight.current = controller
    setPending(target?.type ?? 'create')
    setError('')
    try {
      if (target?.type === 'delete') {
        await deleteComment(target.commentId, token, controller.signal)
      } else if (target?.type === 'edit') {
        await updateComment(target.commentId, content, token, controller.signal)
      } else {
        await createComment(postId, content, token, controller.signal)
      }
      if (controller.signal.aborted) return

      let nextPage = page
      if (!target) {
        setDraft('')
        if (data) nextPage = Math.floor(data.totalElements / commentPageSize)
      } else if (target.type === 'delete' && data?.content.length === 1 && page > 0) {
        nextPage = page - 1
      }
      moveTo(nextPage)
    } catch (error) {
      if (controller.signal.aborted) return
      if (error instanceof ApiError) {
        switch (error.status) {
          case 401:
            onUnauthorized(token)
            return
          case 403:
            if (target) setDenied((ids) => [...ids, target.commentId])
            setAction(null)
            break
          case 404:
            if (!target || error.code === 'STUDY-001') setMissingPost(true)
            moveTo(0)
            break
          case 409:
            if (!target) setClosedByServer(true)
            break
        }
      }
      setError(errorMessage(error))
    } finally {
      inFlight.current = null
      if (!controller.signal.aborted) setPending(null)
    }
  }

  function submitEdit(event: SubmitEvent<HTMLFormElement>, target: CommentAction) {
    event.preventDefault()
    const content = new FormData(event.currentTarget).get('content')
    void mutate(target, typeof content === 'string' ? content : '')
  }

  return (
    <section className="comments-section" aria-labelledby="comments-title">
      <h2 id="comments-title">댓글{data ? ` ${data.totalElements}개` : ''}</h2>
      {error && <p className="error" role="alert">{error}</p>}
      {!postUnavailable && (closed ? <p className="notice">모집이 마감되어 새 댓글을 작성할 수 없습니다.</p>
        : !token ? <p>댓글을 작성하려면 <Link to="/login">로그인</Link>해 주세요.</p>
          : <form className="comment-form" aria-busy={pending === 'create'} onSubmit={(event) => {
            event.preventDefault()
            void mutate(null, draft)
          }}>
            <label htmlFor="new-comment">댓글 내용</label>
            <textarea id="new-comment" rows={3} value={draft} maxLength={500} required
              disabled={pending !== null} onChange={(event) => setDraft(event.target.value)} />
            <button className="primary" type="submit" disabled={pending !== null}>
              {pending === 'create' ? '등록 중…' : '댓글 등록'}
            </button>
          </form>)}

      {!result && <p role="status">댓글을 불러오는 중입니다.</p>}
      {listError !== null && <div>
        <p className="error" role="alert">{errorMessage(listError)}</p>
        {!postUnavailable && <button type="button" disabled={pending !== null} onClick={retry}>댓글 다시 시도</button>}
      </div>}
      {data && (data.content.length === 0 ? <div>
        <p>{data.totalElements === 0 ? '아직 댓글이 없습니다.' : '이 페이지에는 댓글이 없습니다.'}</p>
        {page > 0 && <button type="button" onClick={() => moveTo(0)}>댓글 첫 페이지</button>}
      </div> : <ul className="comment-list">
        {data.content.map((comment) => {
          const current = action?.commentId === comment.commentId ? action : null
          return <li key={comment.commentId}>
            <div className="post-meta">
              <strong>{comment.author.nickname} · {comment.author.generation}기 · {comment.author.part}</strong>
              <time dateTime={comment.createdAt}>{comment.createdAt.slice(0, 16).replace('T', ' ')}</time>
            </div>
            {current?.type === 'edit' ? <form className="comment-form" onSubmit={(event) => submitEdit(event, current)}
              aria-busy={pending === 'edit'}>
              <label htmlFor="edit-comment">댓글 수정 내용</label>
              <textarea id="edit-comment" name="content" defaultValue={comment.content} maxLength={500} required rows={3}
                disabled={pending !== null} />
              <div className="study-actions">
                <button className="primary" type="submit" disabled={pending !== null}>
                  {pending === 'edit' ? '저장 중…' : '댓글 저장'}
                </button>
                <button type="button" disabled={pending !== null} onClick={() => setAction(null)}>취소</button>
              </div>
            </form> : <p className="comment-body">{comment.content}</p>}
            {current?.type === 'delete' && <div className="delete-confirm" role="group" aria-label="댓글 삭제 확인"
              aria-busy={pending === 'delete'}>
              <p>이 댓글을 삭제할까요?</p>
              <div className="study-actions">
                <button className="danger" type="button" disabled={pending !== null} onClick={() => void mutate(current)}>
                  {pending === 'delete' ? '삭제 중…' : '댓글 삭제 확인'}
                </button>
                <button type="button" disabled={pending !== null} onClick={() => setAction(null)}>취소</button>
              </div>
            </div>}
            {token && comment.isAuthor && !current && !denied.includes(comment.commentId) && !postUnavailable &&
              <div className="study-actions">
                <button type="button" disabled={pending !== null} onClick={() => {
                  setError('')
                  setAction({ type: 'edit', commentId: comment.commentId })
                }}>댓글 수정</button>
                <button type="button" className="danger" disabled={pending !== null} onClick={() => {
                  setError('')
                  setAction({ type: 'delete', commentId: comment.commentId })
                }}>댓글 삭제</button>
              </div>}
          </li>
        })}
      </ul>)}
      {data && data.content.length > 0 && data.totalPages > 1 && (
        <nav className="pagination" aria-label="댓글 페이지">
          <button type="button" disabled={page === 0 || pending !== null} onClick={() => moveTo(page - 1)}>이전</button>
          <span aria-current="page">{page + 1} / {data.totalPages}</span>
          <button type="button" disabled={page + 1 >= data.totalPages || pending !== null}
            onClick={() => moveTo(page + 1)}>다음</button>
        </nav>
      )}
    </section>
  )
}
