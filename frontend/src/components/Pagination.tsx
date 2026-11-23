import { Link } from 'react-router'

interface PaginationProps {
  // 0부터 시작하는 현재 페이지
  page: number
  totalPages: number
  // 페이지 번호로 이동할 주소를 만듭니다.
  toPage: (page: number) => string
  label: string
}

// 페이지 이동은 링크로 만듭니다. 현재 페이지는 aria-current로 표시합니다.
export function Pagination({ page, totalPages, toPage, label }: PaginationProps) {
  if (totalPages <= 1) {
    return null
  }

  const linkClassName = 'rounded-md border border-gray-300 bg-white px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-100'
  const pages = Array.from({ length: totalPages }, (_, index) => index)

  return (
    <nav aria-label={label} className="flex flex-wrap items-center justify-center gap-1">
      {page > 0 && (
        <Link to={toPage(page - 1)} className={linkClassName}>
          이전
        </Link>
      )}
      {pages.map((index) =>
        index === page ? (
          <span
            key={index}
            aria-current="page"
            className="rounded-md border border-blue-600 bg-blue-600 px-3 py-1.5 text-sm text-white"
          >
            {index + 1}
          </span>
        ) : (
          <Link key={index} to={toPage(index)} aria-label={`${index + 1}페이지`} className={linkClassName}>
            {index + 1}
          </Link>
        ),
      )}
      {page < totalPages - 1 && (
        <Link to={toPage(page + 1)} className={linkClassName}>
          다음
        </Link>
      )}
    </nav>
  )
}
