import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getComments } from '../../api/comments'
import { getErrorMessage } from '../../api/error'
import type { Comment, StudyStatus } from '../../api/types'
import { useAuth } from '../../auth/useAuth'
import { ErrorAlert } from '../ErrorAlert'
import { Loading } from '../Loading'
import { CommentForm } from './CommentForm'
import { CommentItem } from './CommentItem'
import { COMMENT_PAGE_SIZE, nowAsServerDateTime } from './commentRules'

interface CommentSectionProps {
  postId: number
  // 상세에서 모집 마감/다시 모집을 누르면 바로 바뀌어서 작성 폼과 마감 안내가 전환돼요
  studyStatus: StudyStatus
}

interface CommentListState {
  postId: number
  comments: Comment[]
  loadedPages: number // 지금까지 불러온 페이지 수 (다음에 부를 API page 번호와 같아요)
  totalPages: number
  totalElements: number
  // 불러온 뒤 내가 지운 댓글 수. 지우면 뒤 댓글들이 앞으로 당겨져서 다음 페이지 경계가 밀려요
  removedSinceLoad: number
}

// 기존 목록 뒤에 붙이면서 같은 댓글이 다시 오면 빼요 (그 사이 순서가 밀린 경우)
function mergeComments(current: Comment[], incoming: Comment[]): Comment[] {
  const seen = new Set(current.map((c) => c.commentId))
  return [...current, ...incoming.filter((c) => !seen.has(c.commentId))]
}

export function CommentSection({ postId, studyStatus }: CommentSectionProps) {
  const { status: authStatus, user } = useAuth()
  const [list, setList] = useState<CommentListState | null>(null)
  const [loadError, setLoadError] = useState<{ postId: number; message: string } | null>(null)
  const [loadingMore, setLoadingMore] = useState(false)
  const [moreError, setMoreError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    getComments(postId, 0, COMMENT_PAGE_SIZE)
      .then((page) => {
        if (cancelled) return
        setList({
          postId,
          comments: page.content,
          loadedPages: 1,
          totalPages: page.totalPages,
          totalElements: page.totalElements,
          removedSinceLoad: 0,
        })
      })
      .catch((error: unknown) => {
        if (!cancelled) setLoadError({ postId, message: getErrorMessage(error) })
      })
    return () => {
      cancelled = true
    }
  }, [postId])

  const current = list?.postId === postId ? list : null
  const error = loadError?.postId === postId ? loadError.message : null
  const hasMore = current !== null && current.loadedPages < current.totalPages

  const handleLoadMore = async () => {
    if (!current || loadingMore) return
    setMoreError(null)
    setLoadingMore(true)
    try {
      // 불러온 뒤 지운 댓글이 있으면 경계가 앞으로 밀려서, 앞 페이지도 다시 받아 빠지는 댓글이 없게 해요 (중복은 mergeComments가 걸러요)
      const pages =
        current.removedSinceLoad > 0 ? [current.loadedPages - 1, current.loadedPages] : [current.loadedPages]
      const results = await Promise.all(pages.map((page) => getComments(postId, page, COMMENT_PAGE_SIZE)))
      const last = results[results.length - 1]
      setList((prev) =>
        prev && prev.postId === postId
          ? {
              ...prev,
              comments: results.reduce((acc, page) => mergeComments(acc, page.content), prev.comments),
              loadedPages: prev.loadedPages + 1,
              totalPages: last.totalPages,
              totalElements: last.totalElements,
              removedSinceLoad: 0,
            }
          : prev,
      )
    } catch (err) {
      setMoreError(getErrorMessage(err))
    } finally {
      setLoadingMore(false)
    }
  }

  const handleCreated = (commentId: number, content: string) => {
    if (!current) return
    const countUp = (prev: CommentListState) => {
      const totalElements = prev.totalElements + 1
      return { totalElements, totalPages: Math.ceil(totalElements / COMMENT_PAGE_SIZE) }
    }

    // 오래된 순이라 새 댓글은 맨 끝이에요. 아직 남은 페이지가 있으면 붙이지 않고 더 보기로 보게 안내해요
    if (current.loadedPages < current.totalPages || !user) {
      setNotice('댓글이 등록됐어요. 더 보기로 확인할 수 있어요.')
      setList((prev) => (prev && prev.postId === postId ? { ...prev, ...countUp(prev) } : prev))
      return
    }

    setNotice(null)
    const mine: Comment = {
      commentId,
      content,
      author: { nickname: user.nickname, generation: user.generation, part: user.part },
      isAuthor: true,
      createdAt: nowAsServerDateTime(),
    }
    setList((prev) => {
      if (!prev || prev.postId !== postId) return prev
      const counts = countUp(prev)
      // 붙인 댓글이 새 페이지로 넘어가도 이미 화면에 있으니 불러온 페이지 수도 맞춰요
      return { ...prev, ...counts, comments: [...prev.comments, mine], loadedPages: counts.totalPages }
    })
  }

  const handleUpdated = (commentId: number, content: string) => {
    setList((prev) =>
      prev
        ? { ...prev, comments: prev.comments.map((c) => (c.commentId === commentId ? { ...c, content } : c)) }
        : prev,
    )
  }

  const handleDeleted = (commentId: number) => {
    setList((prev) => {
      if (!prev) return prev
      const totalElements = Math.max(prev.totalElements - 1, 0)
      return {
        ...prev,
        comments: prev.comments.filter((c) => c.commentId !== commentId),
        totalElements,
        totalPages: Math.ceil(totalElements / COMMENT_PAGE_SIZE),
        removedSinceLoad: prev.removedSinceLoad + 1,
      }
    })
  }

  return (
    <section
      aria-labelledby="comments-heading"
      className="mt-6 rounded-xl border border-slate-200 bg-white p-5 shadow-sm sm:p-8"
    >
      <div className="flex items-baseline gap-2">
        {/* 개수를 h2 밖에 둬서 제목 이름이 항상 "댓글"이에요 (E2E에서 찾기 쉽게) */}
        <h2 id="comments-heading" className="text-lg font-bold text-slate-900">
          댓글
        </h2>
        {current && <span className="text-sm text-slate-500">총 {current.totalElements}개</span>}
      </div>

      <div className="mt-2">
        {error ? (
          <div className="mt-2">
            <ErrorAlert message={error} />
          </div>
        ) : !current ? (
          <Loading />
        ) : current.comments.length === 0 ? (
          <p className="py-6 text-center text-sm text-slate-500">아직 신청 댓글이 없어요.</p>
        ) : (
          <ul className="divide-y divide-slate-100">
            {current.comments.map((comment) => (
              <CommentItem
                key={comment.commentId}
                comment={comment}
                onUpdated={handleUpdated}
                onDeleted={handleDeleted}
              />
            ))}
          </ul>
        )}

        {hasMore && (
          <div className="mt-3 space-y-2 text-center">
            <ErrorAlert message={moreError} />
            <button
              type="button"
              onClick={handleLoadMore}
              disabled={loadingMore}
              aria-busy={loadingMore}
              className="w-full rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-500 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {loadingMore ? '불러오는 중...' : '댓글 더 보기'}
            </button>
          </div>
        )}
      </div>

      {notice && (
        <p role="status" className="mt-3 rounded-md border border-green-200 bg-green-50 px-3 py-2 text-sm text-green-700">
          {notice}
        </p>
      )}

      <div className="mt-6 border-t border-slate-100 pt-5">
        {studyStatus === 'CLOSED' ? (
          // 마감 안내가 로그인 여부보다 먼저예요
          <p className="rounded-md bg-slate-100 px-3 py-3 text-center text-sm text-slate-600">
            모집이 마감됐어요. 더 이상 신청할 수 없어요.
          </p>
        ) : authStatus === 'anonymous' ? (
          <p className="rounded-md bg-slate-50 px-3 py-3 text-center text-sm text-slate-600">
            로그인하면 신청할 수 있어요.{' '}
            <Link to="/login" className="font-medium text-indigo-600 hover:underline">
              로그인하고 신청하기
            </Link>
          </p>
        ) : authStatus === 'authenticated' ? (
          <CommentForm postId={postId} onCreated={handleCreated} />
        ) : null}
      </div>
    </section>
  )
}
