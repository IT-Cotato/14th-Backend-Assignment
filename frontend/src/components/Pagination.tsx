interface Props {
  page: number // 0부터 시작
  totalPages: number
  onChange: (page: number) => void
}

export default function Pagination({ page, totalPages, onChange }: Props) {
  if (totalPages <= 1) {
    return null
  }
  return (
    <nav aria-label="페이지 이동" className="mt-4 flex items-center justify-center gap-3 text-sm">
      <button type="button" disabled={page <= 0} onClick={() => onChange(page - 1)}
        className="rounded border border-gray-300 px-3 py-1 hover:bg-gray-100 disabled:opacity-40">
        이전
      </button>
      <span className="text-gray-600">{page + 1} / {totalPages}</span>
      <button type="button" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}
        className="rounded border border-gray-300 px-3 py-1 hover:bg-gray-100 disabled:opacity-40">
        다음
      </button>
    </nav>
  )
}
