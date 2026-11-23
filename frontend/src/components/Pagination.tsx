interface PaginationProps {
  page: number
  totalPages: number
  onChange: (page: number) => void
  label?: string
}

const pageButtonClass =
  'min-w-9 rounded-md border px-2.5 py-1.5 text-sm disabled:cursor-not-allowed disabled:opacity-40'

/** page 는 0부터 시작한다. 화면에는 1부터 보여준다. */
export function Pagination({ page, totalPages, onChange, label = '페이지 이동' }: PaginationProps) {
  if (totalPages <= 1) return null

  const start = Math.max(0, Math.min(page - 2, totalPages - 5))
  const pages = Array.from({ length: Math.min(5, totalPages) }, (_, i) => start + i)

  return (
    <nav aria-label={label} className="mt-6 flex flex-wrap items-center justify-center gap-1">
      <button
        type="button"
        className={`${pageButtonClass} border-stone-300 bg-white`}
        disabled={page === 0}
        onClick={() => onChange(page - 1)}
      >
        이전
      </button>
      {pages.map((p) => (
        <button
          key={p}
          type="button"
          aria-label={`${p + 1}페이지`}
          aria-current={p === page ? 'page' : undefined}
          className={`${pageButtonClass} ${
            p === page ? 'border-amber-500 bg-amber-500 font-semibold text-white' : 'border-stone-300 bg-white'
          }`}
          onClick={() => onChange(p)}
        >
          {p + 1}
        </button>
      ))}
      <button
        type="button"
        className={`${pageButtonClass} border-stone-300 bg-white`}
        disabled={page >= totalPages - 1}
        onClick={() => onChange(page + 1)}
      >
        다음
      </button>
    </nav>
  )
}
