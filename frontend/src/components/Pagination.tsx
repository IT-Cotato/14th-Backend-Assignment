interface PaginationProps {
  // 0부터 시작하는 현재 페이지
  page: number
  totalPages: number
  onChange: (page: number) => void
}

// 이전/다음 페이지 이동. 페이지가 1개 이하면 보여주지 않아요.
export function Pagination({ page, totalPages, onChange }: PaginationProps) {
  if (totalPages <= 1) {
    return null
  }

  const buttonClass =
    'rounded-md border border-gray-300 px-3 py-1 text-sm hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40'

  return (
    <nav aria-label="페이지 이동" className="mt-6 flex items-center justify-center gap-3">
      <button
        type="button"
        onClick={() => onChange(page - 1)}
        disabled={page <= 0}
        className={buttonClass}
      >
        이전
      </button>
      <span className="text-sm text-gray-600">
        {page + 1} / {totalPages}
      </span>
      <button
        type="button"
        onClick={() => onChange(page + 1)}
        disabled={page >= totalPages - 1}
        className={buttonClass}
      >
        다음
      </button>
    </nav>
  )
}
