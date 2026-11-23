import { useCallback, type SubmitEvent } from 'react'
import { Link, useLocation, useSearchParams } from 'react-router'
import { parts } from '../api/auth'
import { ApiError, errorMessage } from '../api/client'
import { listStudyPosts, studyStatusLabels } from '../api/studyPosts'
import { useReadRequest } from '../hooks/useReadRequest'

function StudyPostResults({ query, listSearch, onPage, onReset }: {
  query: string
  listSearch: string
  onPage: (page: number) => void
  onReset: () => void
}) {
  const load = useCallback((signal: AbortSignal) => listStudyPosts(query, signal), [query])
  const { result, retry } = useReadRequest(load)

  if (!result) return <p role="status">모집글을 불러오는 중입니다.</p>
  if ('error' in result) {
    const invalidQuery = result.error instanceof ApiError && result.error.code === 'COMMON-003'
    return <div>
      <p className="error" role="alert">{errorMessage(result.error)}</p>
      <button type="button" onClick={invalidQuery ? onReset : retry}>
        {invalidQuery ? '검색 조건 초기화' : '다시 시도'}
      </button>
    </div>
  }

  const { content, page, totalElements, totalPages } = result.data
  return (
    <>
      <p className="post-count" role="status">모집글 {totalElements}개</p>
      {content.length === 0 ? (
        <div className="empty-posts">
          <p>{totalElements > 0 ? '이 페이지에 모집글이 없습니다.' : '조건에 맞는 모집글이 없습니다.'}</p>
          {totalElements > 0 && <button type="button" onClick={() => onPage(0)}>첫 페이지</button>}
        </div>
      ) : (
        <ul className="post-list">
          {content.map((post) => (
            <li key={post.postId}>
              <span className="post-status" data-status={post.studyStatus}>{studyStatusLabels[post.studyStatus]}</span>
              <h2><Link to={`/study-posts/${post.postId}`} state={{ listSearch }}>{post.title}</Link></h2>
              <div className="post-meta">
                <span>{post.author.nickname} · {post.author.generation}기 · {post.author.part}</span>
                <span>댓글 {post.commentCount}개</span>
                <time dateTime={post.createdAt}>{post.createdAt.slice(0, 10)}</time>
              </div>
            </li>
          ))}
        </ul>
      )}
      {content.length > 0 && totalPages > 1 && (
        <nav className="pagination" aria-label="모집글 페이지">
          <button type="button" disabled={page === 0} onClick={() => onPage(page - 1)}>이전</button>
          <span aria-current="page">{page + 1} / {totalPages}</span>
          <button type="button" disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>다음</button>
        </nav>
      )}
    </>
  )
}

export default function StudyPostListPage({ signedIn }: { signedIn: boolean }) {
  const [params, setParams] = useSearchParams()
  const { search } = useLocation()
  const keyword = params.get('keyword') ?? ''

  function applyFilter(name: 'keyword' | 'status' | 'part', value: string) {
    // The browser URL may already have changed while React commits the previous navigation.
    const next = new URLSearchParams(window.location.search)
    if (value) next.set(name, value)
    else next.delete(name)
    next.set('page', '0')
    setParams(next)
  }

  function submitSearch(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    const value = new FormData(event.currentTarget).get('keyword')
    applyFilter('keyword', typeof value === 'string' ? value.trim() : '')
  }

  function setPage(page: number) {
    const next = new URLSearchParams(window.location.search)
    next.set('page', String(page))
    setParams(next)
  }

  const query = new URLSearchParams()
  for (const name of ['keyword', 'status', 'part', 'page', 'size']) {
    const value = params.get(name)
    if (value !== null) query.set(name, value)
  }
  if (!query.has('page')) query.set('page', '0')
  if (!query.has('size')) query.set('size', '10')
  const requestQuery = query.toString()

  return (
    <section className="panel study-panel" aria-labelledby="study-list-title">
      <div className="study-heading">
        <h1 id="study-list-title">스터디 모집글</h1>
        <Link className="button-link primary" to={signedIn ? { pathname: '/study-posts/new', search } : '/login'}>모집글 작성</Link>
      </div>
      <div className="study-filters">
        <form className="study-search" onSubmit={submitSearch} role="search">
          <label>검색어
            <input key={keyword} type="search" name="keyword" defaultValue={keyword} placeholder="제목 또는 내용" />
          </label>
          <button className="primary" type="submit">검색</button>
        </form>
        <div className="form-row">
          <label>모집 상태
            <select value={params.get('status') ?? ''} onChange={(event) => applyFilter('status', event.target.value)}>
              <option value="">전체 상태</option>
              {Object.entries(studyStatusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
            </select>
          </label>
          <label>작성자 파트
            <select value={params.get('part') ?? ''} onChange={(event) => applyFilter('part', event.target.value)}>
              <option value="">전체 파트</option>
              {parts.map((part) => <option key={part}>{part}</option>)}
            </select>
          </label>
        </div>
      </div>
      <StudyPostResults key={requestQuery} query={requestQuery} listSearch={search}
        onPage={setPage} onReset={() => setParams({})} />
    </section>
  )
}
