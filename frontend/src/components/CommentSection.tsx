import { useEffect, useState, type FormEvent } from 'react'
import { Link, useLocation, useSearchParams } from 'react-router'
import { createComment, getComments } from '../api/comments'
import { getErrorMessage } from '../api/errors'
import type { Comment, PageResponse } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { CommentItem } from './CommentItem'
import { ErrorAlert } from './ErrorAlert'
import { LoadingStatus } from './LoadingStatus'
import { Pagination } from './Pagination'
import { COMMENT_MAX, validateComment } from './commentValidation'
import { errorId, inputClassName, smallPrimaryButtonClassName } from './form'

const PAGE_SIZE = 20

// 모집글 상세 안의 댓글(스터디 신청) 영역입니다. 댓글 페이지는 주소의 ?commentPage= 에 둡니다.
// 마감된 글에 쓰면 서버가 409와 메시지를 내려주고, 그 메시지를 그대로 보여줍니다.
export function CommentSection({ postId }: { postId: number }) {
  const { isLoggedIn } = useAuth()
  const location = useLocation()
  const [searchParams, setSearchParams] = useSearchParams()
  const page = Math.max(0, Number(searchParams.get('commentPage') ?? '0') || 0)

  // 작성, 수정, 삭제 뒤에 숫자를 올려서 목록을 다시 불러옵니다.
  const [reloadCount, setReloadCount] = useState(0)
  const requestKey = JSON.stringify([postId, page, reloadCount])
  const [loaded, setLoaded] = useState<{
    key: string
    result: PageResponse<Comment> | null
    error: string | null
  } | null>(null)
  const isLoading = loaded?.key !== requestKey
  // 다시 불러오는 동안에도 이전 목록은 그대로 보여줍니다.
  const result = loaded?.result ?? null
  const loadError = isLoading ? null : (loaded?.error ?? null)

  const [content, setContent] = useState('')
  const [formError, setFormError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    let cancelled = false
    getComments(postId, page, PAGE_SIZE)
      .then((data) => {
        if (!cancelled) setLoaded({ key: requestKey, result: data, error: null })
      })
      .catch((caught: unknown) => {
        if (!cancelled) setLoaded({ key: requestKey, result: null, error: getErrorMessage(caught) })
      })
    return () => {
      cancelled = true
    }
  }, [requestKey, postId, page])

  const reload = () => setReloadCount((count) => count + 1)

  const toPage = (target: number) => {
    const next = new URLSearchParams(searchParams)
    next.set('commentPage', String(target))
    return `${location.pathname}?${next.toString()}`
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const validationError = validateComment(content)
    if (validationError) {
      setFormError(validationError)
      return
    }
    setFormError(null)
    setIsSubmitting(true)
    try {
      await createComment(postId, { content })
      setContent('')
      // 오래된 순이라 새 댓글은 마지막 페이지에 있습니다.
      const latest = await getComments(postId, 0, PAGE_SIZE)
      const lastPage = Math.max(0, latest.totalPages - 1)
      if (lastPage !== page) {
        const next = new URLSearchParams(searchParams)
        next.set('commentPage', String(lastPage))
        setSearchParams(next)
      } else {
        reload()
      }
    } catch (caught) {
      setFormError(getErrorMessage(caught))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section aria-labelledby="comments-heading" className="flex flex-col gap-4">
      <h2 id="comments-heading" className="text-lg font-bold text-gray-900">
        댓글(스터디 신청){result ? ` ${result.totalElements}개` : ''}
      </h2>

      {isLoggedIn ? (
        <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-2 rounded-lg bg-white p-4 shadow-sm">
          <label htmlFor="comment-new" className="text-sm font-medium text-gray-800">
            댓글 내용
          </label>
          <textarea
            id="comment-new"
            rows={3}
            maxLength={COMMENT_MAX}
            value={content}
            onChange={(event) => setContent(event.target.value)}
            placeholder="댓글을 남기면 스터디 신청이 돼요."
            aria-invalid={formError ? true : undefined}
            aria-describedby={formError ? errorId('comment-new') : undefined}
            className={inputClassName}
          />
          {formError && (
            <p id={errorId('comment-new')} role="alert" className="text-sm text-red-600">
              {formError}
            </p>
          )}
          <div>
            <button type="submit" disabled={isSubmitting} className={smallPrimaryButtonClassName}>
              {isSubmitting ? '작성 중…' : '댓글 작성'}
            </button>
          </div>
        </form>
      ) : (
        <p className="rounded-lg bg-white p-4 text-sm text-gray-600 shadow-sm">
          댓글로 스터디에 신청하려면{' '}
          <Link to="/login" className="font-medium text-blue-600 hover:underline">
            로그인
          </Link>
          해 주세요.
        </p>
      )}

      {isLoading && <LoadingStatus message="댓글을 불러오는 중…" />}
      <ErrorAlert message={loadError} />

      {result &&
        (result.content.length === 0 ? (
          <p className="text-sm text-gray-600">아직 댓글이 없어요.</p>
        ) : (
          <ul aria-label="댓글 목록" className="flex flex-col gap-3">
            {result.content.map((comment) => (
              <CommentItem key={comment.commentId} comment={comment} onChanged={reload} />
            ))}
          </ul>
        ))}

      {result && (
        <Pagination page={result.page} totalPages={result.totalPages} toPage={toPage} label="댓글 페이지" />
      )}
    </section>
  )
}
