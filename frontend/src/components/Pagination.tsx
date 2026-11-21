import { Link } from 'react-router'
import { secondaryButtonClass } from './styles'

interface PaginationProps {
  /** 현재 페이지 (0부터 시작) */
  page: number
  totalPages: number
  /** 해당 페이지로 가는 주소를 만든다 */
  buildHref: (page: number) => string
}

const disabledClass =
  'inline-flex items-center justify-center rounded-md border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-300'

export function Pagination({ page, totalPages, buildHref }: PaginationProps) {
  if (totalPages <= 1) {
    return null
  }
  const hasPrevious = page > 0
  const hasNext = page < totalPages - 1

  return (
    <nav aria-label="페이지 이동" className="flex items-center justify-center gap-3">
      {hasPrevious ? (
        <Link to={buildHref(page - 1)} className={secondaryButtonClass}>
          이전
        </Link>
      ) : (
        <span className={disabledClass}>이전</span>
      )}
      <span className="text-sm text-slate-600">
        {page + 1} / {totalPages}
      </span>
      {hasNext ? (
        <Link to={buildHref(page + 1)} className={secondaryButtonClass}>
          다음
        </Link>
      ) : (
        <span className={disabledClass}>다음</span>
      )}
    </nav>
  )
}
