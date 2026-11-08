interface PaginationProps {
    // 0부터 시작하는 현재 페이지예요.
    page: number
    totalPages: number
    onChange: (page: number) => void
}

const WINDOW = 5

export default function Pagination({ page, totalPages, onChange }: PaginationProps) {
    if (totalPages <= 1) return null

    // 현재 페이지 주변의 최대 5개 페이지 번호를 보여줘요.
    const start = Math.max(0, Math.min(page - Math.floor(WINDOW / 2), totalPages - WINDOW))
    const end = Math.min(totalPages, start + WINDOW)
    const pages = Array.from({ length: end - start }, (_, i) => start + i)

    const base = 'min-w-9 h-9 rounded-lg px-3 text-sm font-medium transition'

    return (
        <nav className="mt-6 flex flex-wrap items-center justify-center gap-1.5" aria-label="페이지 이동">
            <button
                type="button"
                className={`${base} bg-white text-slate-600 ring-1 ring-slate-200 hover:bg-slate-50 disabled:opacity-40`}
                onClick={() => onChange(page - 1)}
                disabled={page === 0}
            >
                이전
            </button>
            {pages.map((p) => (
                <button
                    key={p}
                    type="button"
                    className={`${base} ${
                        p === page
                            ? 'bg-blue-600 text-white'
                            : 'bg-white text-slate-600 ring-1 ring-slate-200 hover:bg-slate-50'
                    }`}
                    onClick={() => onChange(p)}
                    aria-current={p === page ? 'page' : undefined}
                >
                    {p + 1}
                </button>
            ))}
            <button
                type="button"
                className={`${base} bg-white text-slate-600 ring-1 ring-slate-200 hover:bg-slate-50 disabled:opacity-40`}
                onClick={() => onChange(page + 1)}
                disabled={page >= totalPages - 1}
            >
                다음
            </button>
        </nav>
    )
}
