import { Link, useLocation } from 'react-router-dom'

interface PaginationProps {
  page: number // 화면 기준(1부터)
  totalPages: number
}

const linkClass =
  'rounded-md border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-indigo-500'
const disabledClass = 'rounded-md border border-slate-200 px-3 py-2 text-sm font-medium text-slate-300'

// 이동이라 버튼이 아니라 링크로 만들어요. 검색어·필터는 그대로 두고 page만 바꿔요.
// 페이지 번호 링크(1, 2, 10...)는 E2E에서 이름이 겹치기 쉬워서 이전·다음만 둬요.
export function Pagination({ page, totalPages }: PaginationProps) {
  const location = useLocation()
  if (totalPages <= 1) return null

  const hrefFor = (target: number) => {
    const params = new URLSearchParams(location.search)
    if (target <= 1) params.delete('page')
    else params.set('page', String(target))
    const query = params.toString()
    return query ? `${location.pathname}?${query}` : location.pathname
  }

  return (
    <nav aria-label="페이지 이동" className="mt-6 flex items-center justify-center gap-3">
      {page > 1 ? (
        <Link to={hrefFor(page - 1)} className={linkClass}>
          이전
        </Link>
      ) : (
        <span aria-disabled="true" className={disabledClass}>
          이전
        </span>
      )}
      <span className="text-sm text-slate-600" aria-current="page">
        {page} / {totalPages}
      </span>
      {page < totalPages ? (
        <Link to={hrefFor(page + 1)} className={linkClass}>
          다음
        </Link>
      ) : (
        <span aria-disabled="true" className={disabledClass}>
          다음
        </span>
      )}
    </nav>
  )
}
